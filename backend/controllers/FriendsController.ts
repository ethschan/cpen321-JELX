import { Request, Response } from "express";
import { Filter, Document } from "mongodb";
import { fetchProductById, fetchProductImageById } from "./ProductsController";
import { getMessaging, TokenMessage } from 'firebase-admin/messaging';
import { getFirebaseApp, friendsCollection, usersCollection, historyCollection } from "../services";
import { User, FriendRequestBody } from "../types";
import { getHistoryByUserUUID } from "./UsersController";
import { sendNotification } from "../utils";

export class FriendsController {
    
    // POST /friends/requests
    async sendFriendRequest(req: Request<object, object, FriendRequestBody>, res: Response) {
        const { user_uuid: friend_uuid } = req.body;
        const user = req.user as User;
        const user_uuid = user.user_uuid;

        // Check if user is trying to send a friend request to themselves
        if (user_uuid === friend_uuid) {
            return res.status(400).send({message: "Cannot send friend request to yourself."});
        }

        const userFriends = await friendsCollection.findOne({ user_uuid });
        const friendUserDocument = await usersCollection.findOne({ user_uuid: friend_uuid });
        
        // If a user does not have a friend document, they are no longer a user
        if (!friendUserDocument) {
            return res.status(400).send({message: "User does not exist."});
        }

        const targetFriends = await friendsCollection.findOne({ user_uuid: friend_uuid });

        // Check if users are already friends
        if (userFriends?.friends.some(friend => friend.user_uuid === friend_uuid)) {
            return res.status(400).send({message: "Already friends."});
        }

        // Check if friend request has already been sent
        if (!targetFriends?.incoming_requests.some(request => request.user_uuid === user_uuid)) {
            await friendsCollection.updateOne(
                { user_uuid: friend_uuid },
                { $addToSet: { incoming_requests: { user_uuid, name: user.name } } },
                { upsert: true }
            );

            // Send notification to the target user
            await sendNotification(friend_uuid, `${user.name} has sent you a friend request`);

            res.status(200).send({message: "Friend request sent."});
        } else {
            res.status(400).send({message: "Friend request already sent."});
        }
    }

    // POST /friends/requests/accept
    async acceptFriendRequest(req: Request<object, object, FriendRequestBody>, res: Response) {
        const { user_uuid: friend_uuid } = req.body;
        const user = req.user as User;
        const user_uuid = user.user_uuid;

        // Check if user is trying to accept a friend request from themselves
        if (user_uuid === friend_uuid) {
            return res.status(400).send({message: "Cannot accept friend request from yourself."});
        }

        const userFriends = await friendsCollection.findOne({ user_uuid });

        const friend = await usersCollection.findOne({ user_uuid: friend_uuid });

        // Check if friend request exists
        if (friend && userFriends?.incoming_requests.some(request => request.user_uuid === friend_uuid)) {

            await friendsCollection.updateOne(
                { user_uuid },
                { $pull: { incoming_requests: { user_uuid: friend_uuid } }, $addToSet: { friends: { user_uuid: friend_uuid, name: friend.name } } }
            );

            // Ensure the other user has a friends document
            const friendFriends = await friendsCollection.findOne({ user_uuid: friend_uuid });

            if (!friendFriends) {
                await friendsCollection.insertOne({ user_uuid: friend_uuid, friends: [], incoming_requests: [] });
            }

            await friendsCollection.updateOne(
                { user_uuid: friend_uuid },
                { $addToSet: { friends: { user_uuid, name: user.name } } }
            );

            // Send notification to the target user
            await sendNotification(friend_uuid, `${user.name} has accepted your friend request`);
            
            res.status(200).send({message: "Friend request accepted."});
        } else {
            res.status(400).send({message: "No such friend request."});
        }
    }

    // DELETE /friends
    async removeFriend(req: Request<object, object, FriendRequestBody>, res: Response) {
        const { user_uuid: friend_uuid } = req.query;
        const user = req.user as User;
        const user_uuid = user.user_uuid;

        // Check if user is trying to remove themselves as a friend
        if (user_uuid === friend_uuid) {
            return res.status(400).send({message: "Cannot remove yourself as a friend."});
        }
    
        const result = await friendsCollection.updateOne(
            { user_uuid },
            { $pull: { friends: { user_uuid: friend_uuid } } as Filter<Document> }
        );

        const result2 = await friendsCollection.updateOne(
            { user_uuid: friend_uuid },
            { $pull: { friends: { user_uuid } } as Filter<Document> }
        );

        if (result.modifiedCount > 0 && result2.modifiedCount > 0) {
            res.status(200).send({message: "Friend removed."});
        } else {
            res.status(404).send({message: "Friend not found."});
        }
    }

    // DELETE /friends/requests
    async rejectFriendRequest(req: Request<object, object, FriendRequestBody>, res: Response) {
        const { user_uuid: friend_uuid } = req.query;
        const user = req.user as User;
        const user_uuid = user.user_uuid;

        //  Check if user is trying to reject a friend request from themselves
        if (friend_uuid === user_uuid) {
            return res.status(400).send({message: "Cannot reject friend request from yourself"});
        }

        const result = await friendsCollection.updateOne(
            { user_uuid },
            { $pull: { incoming_requests: { user_uuid: friend_uuid as string } } }
        );

        if (result.modifiedCount > 0) {
            res.status(200).send({message: "Friend request rejected."});
        } else {
            res.status(404).send({message: "Friend request not found."});
        }
    }

    // GET /friends/requests
    async getFriendRequests(req: Request<object, object, FriendRequestBody>, res: Response) {
        const user = req.user as User;
        const user_uuid = user.user_uuid;

        const userFriends = await friendsCollection.findOne({ user_uuid });

        if (userFriends?.incoming_requests) {
            res.status(200).send(userFriends.incoming_requests);
        } else {
            res.status(200).send([]);
        }
    }

    // GET /friends/requests/outgoing
    async getOutgoingFriendRequests(req: Request<object, object, FriendRequestBody>, res: Response) {
        const user = req.user as User;
        const user_uuid = user.user_uuid;

        const outgoingRequestDocs = await friendsCollection.find({
            "incoming_requests.user_uuid": user_uuid
        }).toArray();

        const targetUUIDs = outgoingRequestDocs.map(doc => doc.user_uuid);

        // Query only the needed fields
        const users = await Promise.all(
            targetUUIDs.map(async (uuid) => {
                const user = await usersCollection.findOne(
                    { user_uuid: uuid },
                    { projection: { user_uuid: 1, name: 1, _id: 0 } }
                );

                if (user) {
                    return { user_uuid: user.user_uuid, name: user.name };
                }
            })
        );

        res.status(200).send(users);
    }

    // GET /friends
    async getCurrentFriends(req: Request<object, object, FriendRequestBody>, res: Response) {
        const user = req.user as User;
        const user_uuid = user.user_uuid;

        const userFriends = await friendsCollection.findOne({ user_uuid });

        if (userFriends?.friends) {
            res.status(200).send(userFriends.friends);
        } else {
            res.status(200).send([]);
        }
    }

    // GET /friends/history/:user_uuid
    async getFriendHistoryByUUID(req: Request, res: Response) {
        const user = req.user as User;
        const user_uuid = user.user_uuid;
        const { user_uuid: friend_uuid } = req.params;


        const userFriends = await friendsCollection.findOne({ user_uuid });

        if (!userFriends) {
            return res.status(404).send({message: "User does not exist or is not a friend."});
        }

        const friendRelationship = userFriends.friends.find(friend => friend.user_uuid === friend_uuid);

        if (!friendRelationship) {
            return res.status(404).send({message: "User does not exist or is not a friend."});
        }
        
        const friendHistory = await getHistoryByUserUUID(friend_uuid);

        const detailedHistory = await Promise.all(friendHistory.map(async (entry) => {
            const detailedProducts = await Promise.all(entry.products.map(async (product) => {
                const productDetails = await fetchProductById(product.product_id);
                const productImage = await fetchProductImageById(product.product_id);
                return {
                    ...product,
                    "product": {
                        ...productDetails,
                        image: productImage
                    }
                };
            }));
            return {
                ...entry,
                products: detailedProducts
            };
        }));

        res.status(200).send(detailedHistory);
    }

    // POST /friends/notifications
    async sendProductNotification(req: Request, res: Response) {

        getFirebaseApp();

        const { user_uuid: friend_uuid, scan_uuid, message_type } = req.body;
        const user = req.user as User;
        const user_uuid = user.user_uuid;

        if (user_uuid === friend_uuid) {
            return res.status(400).send({message: "Cannot send notification to yourself."});
        }

        const userFriends = await friendsCollection.findOne({ user_uuid });
        const friendRelationship = userFriends?.friends?.find(friend => friend.user_uuid === friend_uuid);

        // Check if user is already friends with the target user
        if (!friendRelationship) {
            return res.status(404).send({message: "User does not exist or is not a friend."});
        }

        const targetUser = await usersCollection.findOne({ user_uuid: friend_uuid });
        const userHistory = await historyCollection.findOne({ user_uuid: friend_uuid });
        
        if (!userHistory) {
            return res.status(404).send({message: "User history not found."});
        }

        const productEntry = userHistory.products.find(product => product.scan_uuid === scan_uuid);

        if (!productEntry) {
            return res.status(404).send({message: "Product not found in user's history."});
        }

        const productDetails = await fetchProductById(productEntry.product_id);

        if (!productDetails) {
            return res.status(404).send({message: "Product not found."});
        }
        const productName = productDetails.product_name;

        // Check message type and create message body
        let messageBody = "";
        if (message_type === "praise") {
            messageBody = `${user.name} has praised you for buying ${productName}`;
        } else if (message_type === "shame") {
            messageBody = `${user.name} has shamed you for buying ${productName}`;
        }

        if (!targetUser || targetUser.fcm_registration_token == "") {
            return res.status(404).send({message: "Target user does not have notifications enabled."});
        }

        const message = {
            notification: {
                title: 'CarbonWise',
                body: messageBody
            },
            token: targetUser.fcm_registration_token
        };

        // Send notification to target user
        getMessaging().send(message as TokenMessage)
        .then(() => {
                res.status(200).send({message: "Notification sent."});
            })
            .catch(() => {
                res.status(500).send({message: "Error sending notification."});
            });
    }

    // GET /friends/ecoscore_score/:user_uuid
    async getFriendEcoscore(req: Request, res: Response) {
        const user = req.user as User;
        const user_uuid = user.user_uuid;
        const { user_uuid: friend_uuid } = req.params;

        const userFriends = await friendsCollection.findOne({ user_uuid });
        const friendRelationship = userFriends?.friends?.find(friend => friend.user_uuid === friend_uuid);

        // Check if user is already friends with the target user
        if (!friendRelationship) {
            return res.status(404).send({message: "User does not exist or is not a friend."});
        }

        const friendHistory = await historyCollection.findOne({ user_uuid: friend_uuid });

        if (!friendHistory) {
            return res.status(404).send({message: "No history found for the friend."});
        }

        res.status(200).send({ ecoscore_score: friendHistory.ecoscore_score });
    }
}


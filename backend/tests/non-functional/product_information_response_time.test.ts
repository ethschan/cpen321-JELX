import supertest from "supertest";
import { performance } from "perf_hooks";
import { createServer } from "../../utils";
import { client } from "../../services";
import { JEST_TIMEOUT_MS } from "../res/data";
// Non-Functional: Test the response time for product information retrieval
describe("Non-Functional: Product Information Response Time", () => {

    beforeAll(async () => {
        await client.connect();
    });

    afterAll(async () => {
        await client.close();
    });

    const app = createServer();

    // Non-Functional Test: Ensure product response time is under 5 seconds and product is returned
    // Input: Valid product ID
    // Expected status code: 200
    // Expected output: Product information is returned within 5 seconds
    test("Product response time is under 5 seconds and product is returned", async () => {
        console.info("Starting test for product response time...");
        const start = performance.now();
        const res = await supertest(app).get("/products/3017620422003");
        const end = performance.now();
        const responseTime = end - start;
        
        console.info("Response time: " + JSON.stringify(responseTime) + " ms");
        console.info("Response status: " + JSON.stringify(res.status));

        expect(responseTime).toBeLessThanOrEqual(5000);
        expect(res.status).toBe(200);
        expect(res.body).toHaveProperty("product");
        expect(res.body.product).toHaveProperty("_id", "3017620422003");
        console.info("Non-Functional Test: Product response time is under 5 seconds and product is returned.");
    }, JEST_TIMEOUT_MS);
});

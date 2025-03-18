import { client } from "../../services";

// Non-Functional: Test the number of products in the database
describe("Non-Functional: Product Database Size", () => {
    const MIN_PRODUCTS = 100000;

    beforeAll(async () => {
        await client.connect();
    });

    afterAll(async () => {
        await client.close();
    });

    // Non-Functional Test: Ensure the number of products in the database is greater than or equal to the minimum required
    // Input: None
    // Expected status code: N/A
    // Expected output: Product count is greater than or equal to 100,000
    test(`Check if the number of products is greater than or equal to ${MIN_PRODUCTS}`, async () => {
        const db = client.db("products_db");
        const collection = db.collection("products");

        const productCount = await collection.countDocuments({
            product_name: { $exists: true },
            categories_tags: { $exists: true },
            categories_hierarchy: { $exists: true },
            countries_tags: { $exists: true },
            lang: { $exists: true }
        }, { limit: MIN_PRODUCTS });

        expect(productCount).toBeGreaterThanOrEqual(MIN_PRODUCTS);
    }, 100000);
});

# Future Implementation Notes

## Conversation and Category Persistence

The current implementation saves conversation categories to a simple JSON file. While this works, a more robust solution should be considered for the future to handle larger datasets and more complex relationships.

### Potential Improvements:

*   **Use a Database:** Migrate the category storage from a JSON file to a Room database.
    *   **Benefits:** Better performance for querying, type safety, and easier to manage relationships if more conversation data (like messages) needs to be persisted in the future.
    *   **Implementation:** Create a `ConversationMetadata` entity with `sender` (primary key) and `category` columns. Create a DAO to interact with this table.
*   **Decouple from `FloatingWindowService`:** The persistence logic is currently initiated within the service. A better architecture would be to use a Repository pattern, where the UI (and the service) interacts with a `ConversationRepository` which in turn uses the database as its data source.
*   **Full Conversation Persistence:** Consider saving all conversation messages to the database to provide a complete chat history even after the service or device restarts.


# API DESIGN - (Microservices + Contracts) (Artifact By: Robin Ghosh @robin.ghosh@microsoft.com)

<img src="media/devday-create-azure-event-subscriber.png">

## Step 8: Azure Cosmos DB Output Binding

The next step in the application architecture is to push a document representing the Event Grid event to **Cosmos DB** for subsequent downstream processing. Adding Cosmos DB requires two steps: 

- Adding an **Output Binding** to the **EventGridTrigger1**
- Updating the  **EventGridTrigger1** function to emit the events into Cosmos DB 

### Step 8.a: Create an Output Binding For the EventTrigger1

Navigate to the **EventGridTrigger1**, select **Integration** and **Add output**: 
- Binding Type: **Azure Cosmos DB**, select **New**, **Cosmos DB account connection**, and link to Cosmos DB account created earlier in the resource group
- Document parameter name: **outputDocument** (case sensitive and must match the outputDocument property in the function 
- Database name: **inDatabase** (as desired)
- Collection name: **MyCollection** (as desired) 
- If true, ..: **Yes** 
- Cosmos DB account connection: **select Cosmos DB account created earlier**  


<img src="media/create-output-binding.png"> 


### Step 8.b: Update Azure Function to set the output binding with the input data being passed

**EventGridTrigger1\index.js** with **outputDocument** set to emit to Cosmos DB output binding: 

````shell

module.exports = async function (context, eventGridEvent) {
    context.log(typeof eventGridEvent);
    context.log(eventGridEvent);

    context.bindings.outputDocument = eventGridEvent.data;
};

````

## Core Services
### 1. Order Service

````shell
POST /orders
GET /orders/{orderId}
GET /orders/{orderId}/status
````

Response
````shell
{
  "orderId": "ORD-123",
  "customerId": "CUST-9",
  "products": [
    {
      "productId": "PROD-456",
      "quantity": 2
    }
  ],
  "status": "IN_PRODUCTION",
  "createdAt": "2026-03-20T10:00:00Z"
}
````

### 2. Product Digital Twin Service
````shell
GET /products/{productId}/lifecycle
GET /products/{productId}/health
````
Response
````shell
{
  "productId": "PROD-456",
  "orderId": "ORD-123",
  "status": "INSTALLED",
  "location": "Atlanta, GA",
  "components": [...],
  "lastServiceDate": "2026-03-10",
  "healthScore": 0.87
}
````
### 3. Event Ingestion API (for external systems)
````shell
POST /events
{
  "eventType": "ProductionStarted",
  "productId": "PROD-456",
  "timestamp": "2026-03-21T08:00:00Z",
  "metadata": {
    "factoryId": "F-1",
    "lineId": "L-2"
  }
}
````

👉 This API pushes into Event Hubs / Service Bus

### 4. AI Query API (Natural Language)
````shell
POST /ai/query
{
  "query": "Why is order ORD-123 delayed?"
}
````
Response
````shell

{
  "answer": "Delay due to supplier shortage in component X",
  "confidence": 0.92,
  "sources": ["supply_chain_log_2026_03"]
}
````
🔁 2) EVENT SCHEMA DESIGN (CRITICAL)

Use a standardized event envelope across all domains.

Base Event Schema (CloudEvents-style)
{
  "eventId": "uuid",
  "eventType": "ProductionStarted",
  "source": "MES",
  "timestamp": "2026-03-21T08:00:00Z",
  "productId": "PROD-456",
  "orderId": "ORD-123",
  "payload": {
    " المصنعId": "F-1",
    "lineId": "L-2"
  }
}
Key Event Types
OrderPlaced
ProductionStarted
QualityCheckPassed
QualityCheckFailed
Shipped
Delivered
Installed
TelemetryReceived
ServicePerformed
FailureDetected

👉 All services subscribe to relevant events.

Event Versioning Strategy
{
  "eventType": "ProductionStarted",
  "version": "v2"
}
Backward compatible changes only
Schema registry (e.g., Avro + registry)
🧠 3) DATA MODEL (DIGITAL TWIN)

Stored in Cosmos DB (NoSQL)

Product Digital Twin Document
{
  "productId": "PROD-456",
  "orderId": "ORD-123",
  "status": "INSTALLED",
  "lifecycle": [
    {
      "stage": "ORDERED",
      "timestamp": "2026-03-20T10:00:00Z"
    },
    {
      "stage": "PRODUCTION",
      "timestamp": "2026-03-21T08:00:00Z"
    }
  ],
  "components": [
    {
      "componentId": "COMP-1",
      "supplierId": "SUP-22",
      "batchId": "B-789"
    }
  ],
  "location": {
    "siteId": "SITE-99",
    "geo": "33.7490,-84.3880"
  },
  "health": {
    "score": 0.87,
    "lastUpdated": "2026-03-23T12:00:00Z"
  }
}
Time-Series IoT Schema (Azure Data Explorer)
{
  "timestamp": "2026-03-23T12:00:00Z",
  "productId": "PROD-456",
  "temperature": 78.5,
  "pressure": 30.2,
  "vibration": 0.02
}
Graph Relationships (Optional but powerful)
Product → Component → Supplier
Product → Service Event → Technician
Product → Failure → Root Cause
🤖 4) AI / ML MODEL CHOICES
A. Predictive Maintenance
Model Options
Gradient Boosted Trees (XGBoost, LightGBM)
LSTM (for time-series)
Transformer-based time-series models (advanced)
Features
Sensor data (rolling averages, trends)
Usage patterns
Environmental conditions
Service history
Output
{
  "productId": "PROD-456",
  "failureProbability": 0.78,
  "predictedFailureWindowDays": 14
}
B. Anomaly Detection
Techniques
Isolation Forest
Autoencoders
Statistical thresholds (baseline)

Used for:

Manufacturing defects
Sensor anomalies
C. Delay Prediction (Supply Chain)
Model
Classification / regression (XGBoost)
Inputs
Supplier performance
Inventory levels
Historical delays
External signals (weather, logistics)
D. Generative AI (RAG Architecture)
Stack
Azure OpenAI (LLM)
Azure Cognitive Search (vector DB)
RAG Pipeline
User Query
   ↓
Embedding Model
   ↓
Vector Search (manuals, logs, tickets)
   ↓
Context Retrieval
   ↓
LLM (grounded response)
Example Retrieved Context
Service manual PDF
Past failure tickets
Technician notes
🧠 Prompt Engineering (Example)
You are an industrial maintenance assistant.

Context:
- Product: PROD-456
- Recent issue: overheating
- Sensor data: temperature rising steadily

Question:
What is the likely cause and recommended fix?
⚙️ 5) DATA PIPELINE (END-TO-END)
Real-Time Flow
IoT Device → IoT Hub → Event Hubs → Stream Processing (Databricks)
→ Feature Store → ML Model → Prediction → Cosmos DB
Batch Flow
Data Lake → Data Factory → Synapse → BI / Model Training
🔐 6) OBSERVABILITY + RELIABILITY
Monitoring
Azure Monitor
Application Insights
Key Metrics
Event lag
Model accuracy
API latency
Data freshness
Resilience Patterns
Retry + dead-letter queues (Service Bus)
Idempotent event processing
Circuit breakers (API layer)
🎯 Final “Senior-Level” Insight

If you want to stand out, say this:

“The hardest part isn’t the AI—it’s building a consistent event model and digital thread. Once that’s in place, AI becomes a force multiplier across every lifecycle stage.”

If you want, I can go even further into:

Database partitioning strategies (Cosmos DB RU optimization)
Exact feature engineering examples
Terraform / deployment architecture
Sequence diagrams for one lifecycle event

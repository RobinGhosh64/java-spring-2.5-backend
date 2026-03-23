
# API DESIGN - (Microservices + Contracts) (Artifact By: Robin Ghosh @robin.ghosh@microsoft.com)


We design domain-driven microservices, each owning its own data and exposing APIs.
<img src="media/devday-select-create-storage-account.png">


## Step 6: Ask Azure to wire up this Blob Storage Account to emit Events when a new blob is added to this container

Important steps to follow, please pay attention.


### Step 6a: Ensure the Azure subscription has **Registered** the *Microsoft.EventGrid resource provider* 

- Navigate in the Azure portal to the **Subscription**
- Select the **Settings->Resource providers**
- Filter on **EventGrid** 
- **Verify** or **Check**: **Status=Registered**

<img src="media/sub.eventgrid.registered.png">

### Step 6b: Ask Azure to create a Event Subscriber(consumer) to trap events, push to EventGrid Topic and then call our Azure Function for post processing 

Navigate to the Resource Group created previously, select the **devdaymystorage** storage account, Click on **Events** <img src="media/rg.events.select.png" > icon and thenk click on **Event Subscription** on top, to create a new consumer. 

<img src="media/devday-create-azure-event-subscriber.png">


### Step 7: Event Grid Blob Storage Light Test

Current status is the following have been created and ready for testing: 

- Azure Blob storage account 
- Event Grid Topic created by Azure
- Function App to receive and log events 

Next step is to create an blob container, upload files and verify the Event Grid System Topic triggers the Function App 

- Navigate to the Resource Group, select our Blob storage account **devdayfebmystorage**
- Select: **Containers**, **+ Add Container**
- Name: **test**, 
- Access level: **default** or **as desired** 

Click on **Review + create** and then confirm final creation

Open a second browser session in the Azure Portal:
- Session 1: Navigate to the newly created **test** container1
- Session 2: Navigate to the Function App, **EventGridTrigger1**, and open the **Logs** menu, to view the Function logs 
- **Blob container**, select **Upload**, upload a favorite file, image or related media:

<img src="media/upload-blob-test.png"> 

-  **EventGridTrigger1**, observe for each image, Event Grid will trigger the Fuction, **Logs** will reflect the Event Grid trigger content: 

<img src="media/output-binding-log.png"> 


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

Core Services
1. Order Service
POST /orders
GET /orders/{orderId}
GET /orders/{orderId}/status

Response

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
2. Product Digital Twin Service
GET /products/{productId}/lifecycle
GET /products/{productId}/health

Response

{
  "productId": "PROD-456",
  "orderId": "ORD-123",
  "status": "INSTALLED",
  "location": "Atlanta, GA",
  "components": [...],
  "lastServiceDate": "2026-03-10",
  "healthScore": 0.87
}
3. Event Ingestion API (for external systems)
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

👉 This API pushes into Event Hubs / Service Bus

4. AI Query API (Natural Language)
POST /ai/query
{
  "query": "Why is order ORD-123 delayed?"
}

Response

{
  "answer": "Delay due to supplier shortage in component X",
  "confidence": 0.92,
  "sources": ["supply_chain_log_2026_03"]
}
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

# Case Study Scenarios to discuss

## Scenario 1: Cost Allocation and Tracking
**Situation**: The company needs to track and allocate costs accurately across different Warehouses and Stores. The costs include labor, inventory, transportation, and overhead expenses.

**Task**: Discuss the challenges in accurately tracking and allocating costs in a fulfillment environment. Think about what are important considerations for this, what are previous experiences that you have you could related to this problem and elaborate some questions and considerations

**Questions you may have and considerations:**
- **Define the “cost object” and granularity**
  - What do we want to allocate to: store, warehouse, lane (warehouse→store), order, shipment, SKU/product, return, or a mix?
  - Do we need daily operational views vs a finance “period close” view (provisional vs finalized)?

- **Agree on allocation logic (and what “accurate” means)**
  - Which costs are direct (carrier invoice for a shipment) vs indirect (rent, shared labor, shared equipment)?
  - What drivers are acceptable: picks, units, weight, cube, storage days, labor minutes, distance, touches?
  - Do we need finance-grade auditability (reproducible, explainable) or decision-support speed (often both)?

- **Source systems + data quality**
  - What are the sources of truth (WMS/OMS/TMS, payroll/timekeeping, AP/ERP/GL)?
  - How do we handle late data, missing scans, duplicates, cancellations/returns, and manual adjustments?
  - Data contract: required fields (timestamp, warehouse, store, cost type, currency, amount/unit, source, correlation IDs).

- **Inventory cost specifics**
  - Valuation method: FIFO/LIFO/weighted average; standard vs actual cost.
  - Transfers between warehouses, shrink/damage, write-offs, and returns handling.

- **Labor tracking**
  - Do we have activity-based time (pick/pack/replenishment) or only shift totals?
  - How should mixed work be split across stores/SKUs (especially for shared zones/lines)?

- **Transportation complexity**
  - Allocate from actual invoices vs rate cards with later true-ups?
  - Multi-drop routes: how to distribute cost across stops fairly and consistently?

- **Overhead + governance**
  - What overhead is warehouse-specific vs corporate/shared, and should shared overhead be allocated at all?
  - Audit trail: who can override, how overrides are logged, and how to explain “why did cost X land on store Y?”

- **Experience I’d relate**
  - I’ve seen cost initiatives fail when ops and finance define metrics differently. The best results came from agreeing early on drivers/cutoff rules and keeping a transparent audit trail plus “provisional vs finalized” numbers.

## Scenario 2: Cost Optimization Strategies
**Situation**: The company wants to identify and implement cost optimization strategies for its fulfillment operations. The goal is to reduce overall costs without compromising service quality.

**Task**: Discuss potential cost optimization strategies for fulfillment operations and expected outcomes from that. How would you identify, prioritize and implement these strategies?

**Questions you may have and considerations:**
- **How I’d identify opportunities**
  - Establish a baseline: cost per order, per unit, per shipment, per pick, per storage day, split by warehouse, store, lane, and product family.
  - Break down costs into buckets: labor, transportation, inventory carrying/shrink, overhead, returns.
  - Find outliers (high-cost lanes, warehouses with low pick rates, stores with high return rates, “expensive” SKUs that drive touches).

- **Potential optimization strategies**
  - **Labor**: slotting, pick-path optimization, batching/waves, standard work + training, staffing to demand, targeted automation where ROI is clear.
  - **Transport**: consolidation to reduce partial shipments, carrier mix/rate negotiation, tender optimization, zone skipping, cartonization improvements.
  - **Inventory**: smarter allocation to reduce split shipments, reduce emergency transfers, reduce aging stock/write-offs, improve replenishment.
  - **Overhead**: space utilization, energy optimization, maintenance planning.
  - **Quality/returns**: reduce mis-picks/damage (often cheaper than shipping optimizations).

- **How I’d prioritize**
  - Impact/effort + risk to SLA: expected savings, time-to-value, operational disruption, dependencies.
  - Start with “no regret” items (measurement and process improvements) before large structural changes.

- **How I’d implement**
  - Pilot in one warehouse/lane, define success metrics, run a time-boxed trial with a rollback plan.
  - Validate savings with finance (avoid “paper savings”), then roll out gradually.
  - Put a cadence in place: dashboards + weekly review to sustain improvements.

- **Expected outcomes**
  - Short term: better visibility and quick wins.
  - Medium term: reduced cost per shipment/order without harming service levels.
  - Long term: early anomaly detection and data-driven decision-making.

## Scenario 3: Integration with Financial Systems
**Situation**: The Cost Control Tool needs to integrate with existing financial systems to ensure accurate and timely cost data. The integration should support real-time data synchronization and reporting.

**Task**: Discuss the importance of integrating the Cost Control Tool with financial systems. What benefits the company would have from that and how would you ensure seamless integration and data synchronization?

**Questions you may have and considerations:**
- **Why integration matters**
  - Removes manual reconciliation and increases trust in the numbers.
  - Enables consistent reporting: operations gets near-real-time signals; finance gets period-accurate actuals.
  - Improves auditability: link operational drivers to invoices and GL postings.

- **Benefits to the company**
  - Faster close, fewer disputes between teams, better profitability visibility by warehouse/store/lane.
  - Better forecasting because actuals are consistent and timely.

- **What I’d integrate with (typical landscape)**
  - ERP/GL (cost centers, postings), AP (carrier invoices), procurement, payroll/timekeeping.
  - WMS/OMS/TMS for operational events and shipment/order facts.

- **How to ensure seamless integration**
  - Clear data contracts and ownership (who owns which fields and definitions).
  - Idempotency and deduplication (unique keys), schema versioning, replay/backfill support.
  - Separate “estimated real-time” from “finalized accounting” with true-up flows for late invoices/adjustments.

- **Synchronization + controls**
  - Reconciliation checks so totals match ERP/GL within agreed tolerances; exceptions flagged with traceability.
  - Security and governance: access control (e.g., labor cost visibility), audit logs for changes and overrides.

## Scenario 4: Budgeting and Forecasting
**Situation**: The company needs to develop budgeting and forecasting capabilities for its fulfillment operations. The goal is to predict future costs and allocate resources effectively.

**Task**: Discuss the importance of budgeting and forecasting in fulfillment operations and what would you take into account designing a system to support accurate budgeting and forecasting?

**Questions you may have and considerations:**
- **Why budgeting/forecasting is critical**
  - Fulfillment cost is highly variable with volume and constraints; forecasting prevents reactive staffing/shipping decisions.
  - Enables capacity planning (labor, space, carriers) and early variance detection.

- **Key inputs I’d consider**
  - Demand forecast by store/product (seasonality, promotions).
  - Operational constraints: warehouse capacity, labor availability, carrier capacity, SLA targets.
  - Cost drivers: pick/pack rate, storage days, distance/lane cost, returns rate, shrink.
  - Planned changes: new stores, assortment changes, warehouse ramp-ups/closures.

- **System design considerations**
  - Driver-based model that supports what-if scenarios (“if volume +15% in WH A, what happens to labor/transport?”).
  - Multi-level forecasts (warehouse, store, lane, product family) and versioning of assumptions.
  - Variance analysis: budget vs forecast vs actual, with drill-down into drivers (volume vs efficiency vs rate changes).

- **Practical approach**
  - Start with simple, explainable driver models, then improve as data quality matures.
  - Keep “provisional vs finalized” consistent with finance close, and recalibrate regularly using actuals.

## Scenario 5: Cost Control in Warehouse Replacement
**Situation**: The company is planning to replace an existing Warehouse with a new one. The new Warehouse will reuse the Business Unit Code of the old Warehouse. The old Warehouse will be archived, but its cost history must be preserved.

**Task**: Discuss the cost control aspects of replacing a Warehouse. Why is it important to preserve cost history and how this relates to keeping the new Warehouse operation within budget?

**Questions you may have and considerations:**
- **Why preserving cost history matters**
  - Needed for auditability and for comparing performance over time (cost per order/unit, productivity, overhead absorption).
  - Without history, it’s hard to quantify whether the replacement improved things or just changed reporting.

- **Core challenge**
  - Reusing the same business unit code can blur “old vs new” warehouse performance if history is merged incorrectly.

- **How I’d handle it**
  - Model two concepts:
    - Business-facing identifier (business unit code)
    - Operational warehouse “instance/version” with effective dates (old archived instance vs new active instance)
  - Keep cost entries linked to the instance/version, while reporting can still roll up under the shared code when needed.

- **Budget control implications**
  - Define a baseline for the new warehouse that reflects ramp-up (productivity dip is common during transition).
  - Track one-time transition costs separately from steady-state run-rate.
  - Compare budget vs actual with clear segmentation (legacy vs new instance), so corrective actions target the right drivers.

- **Questions I’d clarify up front**
  - How should reporting behave: continuity under one code, split views, or both?
  - How are capex/depreciation treated (part of warehouse overhead or tracked separately)?
  - What is the acceptable ramp-up period and how does that affect service/cost targets?

## Instructions for Candidates
Before starting the case study, read the [BRIEFING.md](BRIEFING.md) to quickly understand the domain, entities, business rules, and other relevant details.

**Analyze the Scenarios**: Carefully analyze each scenario and consider the tasks provided. To make informed decisions about the project's scope and ensure valuable outcomes, what key information would you seek to gather before defining the boundaries of the work? Your goal is to bridge technical aspects with business value, bringing a high level discussion; no need to deep dive.

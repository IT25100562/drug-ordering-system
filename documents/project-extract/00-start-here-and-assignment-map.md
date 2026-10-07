# MediSys project extract: start here

This folder contains evidence-based notes and assignment-ready starting material for the
**IT2140 Database Design and Development — Assignment Part 02**. It is based on the
project source present in this repository, especially `database/schema.sql`,
`database/sample-data.sql`, `README.md`, and the two assignment Markdown files.

## Project at a glance

**MediSys** is a group-built online medicine ordering application. Customers browse a
medicine catalog, manage a cart/wishlist, submit prescriptions for pharmacist review,
pay for cart or approved prescription orders, and track deliveries. Admin, pharmacist,
and delivery-staff accounts have different work areas.

| Area | Project evidence |
|---|---|
| Application | Java 17 web application using Jakarta Servlets and JSP |
| Database | Microsoft SQL Server; relational schema implemented in T-SQL |
| Data access | JDBC DAO interfaces and implementations |
| Build/deployment | Maven WAR deployed to Apache Tomcat 11 |
| Database scripts | `database/create-database.sql`, `database/schema.sql`, `database/sample-data.sql` |
| Database tables | 14 tables across users, catalog, cart, orders, prescriptions, and delivery |
| High-value database behaviors | PK/FK/CHECK/UNIQUE constraints, filtered index, indexes, transactions, status histories |

## Assignment and rubric map

| Part | Weight | What earns the top band | Extract to use |
|---|---:|---|---|
| A. EER → relational mapping | 10 | All entities, keys, relationships, constraints, and ISA choices justified; refinements explained | [01-schema-and-mapping.md](01-schema-and-mapping.md) |
| B. SQL DDL | 20 | Complete, correct DDL with appropriate data types and constraints | [02-ddl-and-integrity.md](02-ddl-and-integrity.md) |
| C. Sample data | 10 | At least five valid records in **every** table plus clear `SELECT *` screenshots | [03-sample-data-audit.md](03-sample-data-audit.md) |
| D. Queries and outputs | 20 | Correct examples of all five required query types, explained and evidenced with outputs | [04-query-pack.md](04-query-pack.md) |
| E. Stored function/procedure | 15 | Relevant routine runs successfully with sample input and output | [05-routine-and-trigger-proposals.md](05-routine-and-trigger-proposals.md) |
| F. Trigger | 15 | Relevant trigger runs successfully and its effect is demonstrated | [05-routine-and-trigger-proposals.md](05-routine-and-trigger-proposals.md) |
| G. Viva/demo | 10 | Each member confidently runs and explains SQL and design decisions | [06-viva-preparation.md](06-viva-preparation.md) |

The rubric's excellent bands are 9–10, 18–20, 9–10, 18–20, 13–15, 13–15, and
9–10 respectively. The weights total 100 marks. The final submission is one PDF named
`GroupID_Assignment01_Part02.pdf`; the guideline gives 27 September 2026 as the
submission deadline and viva beginning 28 September 2026.

## Evidence versus proposed work

- **Verified project facts** are summarized from committed project files. The schema and
  seed data can be cross-checked directly in the paths cited in each extract.
- **Proposed query/routine/trigger material** is assignment support, not code currently
  implemented in the application. In particular, the stored procedure and trigger in
  document 05 are drafts to add and demonstrate in SQL Server before claiming they work.
- The project extract does **not** contain the original EER drawing. Document 01 explains
  the relational design visible in the DDL; compare it against the group's actual EER
  diagram and update the diagram/mapping if the original differs.
- This repository's SQL scripts target **Microsoft SQL Server**, not MySQL or SQLite.
  They use `IDENTITY`, `NVARCHAR`, `BIT`, `DATETIME2`, `SYSDATETIME()`, `DATEADD`,
  `GO`, filtered indexes, and SQL Server-specific locking hints.
- Do not present suggested query output as a captured/verified screenshot. Run each
  statement on the assignment database and capture the real query and result grid.

## Recommended document-making order

1. Finalize the EER and mapping table using document 01.
2. Include and explain the DDL refinements/constraints using document 02.
3. Address the seed-data shortfalls before taking the per-table screenshots in document 03.
4. Run the query pack and capture actual output/plan evidence in document 04.
5. Add and execute one procedure and one trigger using document 05.
6. Rehearse the concepts and SQL without notes using document 06.

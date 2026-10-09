# Database documentation

PostgreSQL stores authoritative business records. Neo4j stores a derived, rebuildable recommendation graph.

## Schema references

- [Subsystem 1: catalog](relational-model-subsystem-1.md)
- [Subsystem 2: accounts, employees, and events](relational-model-subsystem-2.md)
- [Subsystem 3: sales, visits, and recommendations](relational-model-subsystem-3.md)
- [Local seed data](demo-data-local.md)

SQL table names, column names, and enum values retain their implemented identifiers, including Serbian domain terms. Explanatory text is in English.

## Historical verification records

These reports describe earlier implementation stages, not the current test status:

- [Subsystem 1 verification](subsystem-1-verification.md)
- [Subsystem 2 verification](subsystem-2-verification.md)
- [Subsystem 3 verification](subsystem-3-verification.md)

Use the [root guide](../README.md) for current startup commands and the [submission audit](../docs/submission-readiness.md) for the latest recorded integrated review. Never reset existing database volumes as part of routine startup or testing.

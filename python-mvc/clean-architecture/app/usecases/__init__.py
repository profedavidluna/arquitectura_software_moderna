"""
Use Cases Layer - Application Business Rules

In Clean Architecture, use cases contain APPLICATION-SPECIFIC business rules.
Each use case is a single class with a single execute() method (SRP).

Key principles:
- One class per use case (Single Responsibility)
- Depends on entities (inner layer) and gateway interfaces
- Gateway interfaces are DEFINED here but IMPLEMENTED in outer layers
- This layer orchestrates the flow of data to/from entities

The use cases layer defines the gateway interface (ABC) that outer layers
must implement. This is the Dependency Inversion Principle in action.
"""

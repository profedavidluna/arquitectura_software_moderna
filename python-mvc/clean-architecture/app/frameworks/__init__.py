"""
Frameworks Layer (Outermost Layer)

In Clean Architecture, this is the outermost layer containing:
- Database implementations (asyncpg, SQLAlchemy, etc.)
- Web framework setup (FastAPI)
- External service clients

This layer implements the interfaces defined in inner layers.
It's the most volatile layer - frameworks change frequently,
but inner layers remain stable.
"""

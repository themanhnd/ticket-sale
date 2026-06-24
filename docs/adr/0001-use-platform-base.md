# ADR 0001 - Use platform base before business services

## Status
Accepted

## Context
Project will have multiple services. Without a shared platform base, config, routing, logging, and dependency management can drift quickly.

## Decision
Build platform base first:
- discovery
- config server
- gateway
- common module

## Consequences
Positive:
- service creation becomes more consistent
- public boundary is clear
- config is centralized early

Trade-off:
- more upfront setup
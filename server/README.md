# Code Review Agent Server

Java 21 and Spring Boot 3.5 service for receiving code-review requests, coordinating the Agent-RAG service, streaming progress, and storing results.

## Modules

| Package | Responsibility |
| --- | --- |
| `controller` | HTTP APIs, request validation, and response mapping |
| `application` | Review, result query, feedback, and question use cases |
| `domain` | Business models, rules, states, and repository ports |
| `infrastructure` | PostgreSQL, Redis, Flyway, storage, and event adapters |
| `client` | Calls to the Node.js Agent-RAG service |
| `config` | Spring and environment configuration |
| `common` | Error contracts, IDs, constants, and shared utilities |

## Commands

```shell
./mvnw spring-boot:run
```

On Windows, use `mvnw.cmd` instead of `./mvnw`.

This project does not include automated test code or test framework dependencies.

## Development Rules

- 每个方法必须添加简洁的中文 Javadoc 注释。
- 每个业务对象字段必须添加简洁的中文 Javadoc 注释。
- 异常、参数校验、接口响应和日志消息必须使用中文。

<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Test</title>
</head>
<body>
    You're currently on route: "<span th:text="${request_uri}"></span>"
    <br/>
    Your session id is: <span th:text="${session.id}"></span>
    <br/>
    <span th:text="${message}"></span>
</body>
</html>
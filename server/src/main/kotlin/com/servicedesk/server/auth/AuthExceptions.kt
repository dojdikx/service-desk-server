package com.servicedesk.server.auth

class ValidationException(message: String) : RuntimeException(message)

class EmailAlreadyExistsException(email: String) : RuntimeException("Email already exists: $email")

class InvalidCredentialsException : RuntimeException("Invalid credentials")

class InactiveAccountException : RuntimeException("Account is inactive")

class StaffNotFoundException : RuntimeException("Staff not found")

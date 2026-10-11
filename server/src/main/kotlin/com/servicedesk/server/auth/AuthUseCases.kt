package com.servicedesk.server.auth

class RegisterStaffUseCase(
    private val repository: StaffAuthRepository,
) {
    suspend fun executeAsAdmin(request: RegisterRequest): RegisterStaffResponse {
        val staff = createStaff(request)
        return RegisterStaffResponse(staff = staff.toPublicDto())
    }

    suspend fun execute(request: RegisterRequest): AuthResponse {
        val staff = createStaff(request)
        return AuthResponse(
            token = JwtService.issue(staff.id, staff.email, staff.role),
            staff = staff.toPublicDto(),
        )
    }

    private suspend fun createStaff(request: RegisterRequest): StaffRecord {
        val email = request.email.trim().lowercase()
        validateEmail(email)
        validatePassword(request.password)
        if (repository.findByEmail(email) != null) {
            throw EmailAlreadyExistsException(email)
        }
        return repository.create(
            email = email,
            passwordHash = PasswordHasher.hash(request.password),
            firstName = request.firstName,
            lastName = request.lastName,
        )
    }
}

class LoginStaffUseCase(
    private val repository: StaffAuthRepository,
) {
    suspend fun execute(request: LoginRequest): AuthResponse {
        val email = request.email.trim().lowercase()
        val staff = repository.findByEmail(email) ?: throw InvalidCredentialsException()
        if (!staff.isActive) throw InactiveAccountException()
        if (!PasswordHasher.verify(request.password, staff.passwordHash)) {
            throw InvalidCredentialsException()
        }
        return AuthResponse(
            token = JwtService.issue(staff.id, staff.email, staff.role),
            staff = staff.toPublicDto(),
        )
    }
}

class GetCurrentStaffUseCase(
    private val repository: StaffAuthRepository,
) {
    suspend fun execute(staffId: Int): MeResponse {
        val staff = repository.findById(staffId) ?: throw StaffNotFoundException()
        if (!staff.isActive) throw InactiveAccountException()
        return MeResponse(staff = staff.toPublicDto())
    }
}

private fun validateEmail(email: String) {
    if (email.isBlank() || !email.contains("@") || !email.contains(".")) {
        throw ValidationException("Укажите корректный email")
    }
}

private fun validatePassword(password: String) {
    if (password.length < 6) {
        throw ValidationException("Пароль должен быть не короче 6 символов")
    }
}

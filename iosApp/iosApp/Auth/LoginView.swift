import SwiftUI
import shared

struct LoginView: View {
    @State private var viewModel = LoginViewModelWrapper()
    let onDismiss: () -> Void

    @State private var email: String = ""
    @State private var password: String = ""
    @State private var showPassword: Bool = false
    @FocusState private var focusedField: Field?

    private enum Field: Hashable {
        case email, password
    }

    var body: some View {
        ZStack {
            Color.strakkBackground.ignoresSafeArea()

            VStack(spacing: 0) {
                toolbar

                ScrollView {
                    VStack(spacing: StrakkSpacing.lg) {
                        header
                        emailField
                        passwordField
                    }
                    .padding(.horizontal, StrakkSpacing.xl)
                    .padding(.top, StrakkSpacing.xl)
                }

                VStack(spacing: StrakkSpacing.md) {
                    if let error = viewModel.state.error {
                        Text(error)
                            .font(.strakkCaption)
                            .foregroundStyle(Color.strakkError)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, StrakkSpacing.xl)
                            .transition(.opacity)
                    }

                    StrakkPrimaryButton(
                        title: viewModel.state.isLoading ? "Signing in…" : "Sign in",
                        action: {
                            focusedField = nil
                            viewModel.send(LoginEventOnLogin())
                        },
                        isEnabled: !viewModel.state.isLoading
                    )
                    .padding(.horizontal, StrakkSpacing.xl)
                    .accessibilityLabel(Text("Sign in"))

                    signUpLink
                }
                .padding(.bottom, StrakkSpacing.xxl)
            }
        }
        .alert(Text("Info"), isPresented: Binding(
            get: { viewModel.infoMessage != nil },
            set: { if !$0 { viewModel.infoMessage = nil } }
        )) {
            Button("OK") { viewModel.infoMessage = nil }
        } message: {
            Text(viewModel.infoMessage ?? "")
        }
        .task {
            viewModel.onNavigateToHome = { onDismiss() }
            viewModel.onNavigateToOnboarding = { onDismiss() }
            viewModel.onNavigateToSignUp = { onDismiss() }
        }
        .onAppear {
            email = viewModel.state.email
            password = viewModel.state.password
        }
    }

    private var toolbar: some View {
        HStack {
            StrakkCloseButton(action: onDismiss)
            Spacer()
        }
        .padding(.horizontal, StrakkSpacing.xs)
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            Text("Welcome back")
                .font(.strakkHeading1)
                .foregroundStyle(Color.strakkTextPrimary)

            Text("Sign in to access your data.")
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextSecondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var emailField: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            Text("Email")
                .font(.strakkCaptionBold)
                .foregroundStyle(Color.strakkTextSecondary)

            TextField("you@email.com", text: $email)
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextPrimary)
                .keyboardType(.emailAddress)
                .autocapitalization(.none)
                .autocorrectionDisabled()
                .submitLabel(.next)
                .padding(.horizontal, StrakkSpacing.sm)
                .padding(.vertical, StrakkSpacing.sm)
                .background(Color.strakkSurface1)
                .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
                .overlay(
                    RoundedRectangle(cornerRadius: StrakkRadius.sm)
                        .strokeBorder(Color.strakkBorderFaint, lineWidth: 1)
                )
                .focused($focusedField, equals: .email)
                .onChange(of: email) { _, newValue in
                    viewModel.send(LoginEventOnEmailChanged(email: newValue))
                }
                .onSubmit { focusedField = .password }
        }
    }

    private var passwordField: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            HStack {
                Text("Password")
                    .font(.strakkCaptionBold)
                    .foregroundStyle(Color.strakkTextSecondary)

                Spacer()

                Button {
                    viewModel.send(LoginEventOnForgotPassword())
                } label: {
                    Text("Forgot password?")
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkPrimary)
                }
                .frame(height: 32)
                .accessibilityLabel(Text("Reset your password"))
            }

            HStack {
                Group {
                    if showPassword {
                        TextField("Your password", text: $password)
                    } else {
                        SecureField("Your password", text: $password)
                    }
                }
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextPrimary)
                .autocapitalization(.none)
                .autocorrectionDisabled()
                .submitLabel(.done)
                .focused($focusedField, equals: .password)
                .onChange(of: password) { _, newValue in
                    viewModel.send(LoginEventOnPasswordChanged(password: newValue))
                }
                .onSubmit {
                    focusedField = nil
                    viewModel.send(LoginEventOnLogin())
                }

                Button {
                    showPassword.toggle()
                } label: {
                    Image(systemName: showPassword ? "eye.slash" : "eye")
                        .foregroundStyle(Color.strakkTextSecondary)
                        .frame(width: 44, height: 44)
                }
                .accessibilityLabel(Text(showPassword ? "Hide password" : "Show password"))
            }
            .padding(.horizontal, StrakkSpacing.sm)
            .padding(.vertical, StrakkSpacing.xs)
            .background(Color.strakkSurface1)
            .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
            .overlay(
                RoundedRectangle(cornerRadius: StrakkRadius.sm)
                    .strokeBorder(Color.strakkBorderFaint, lineWidth: 1)
            )
        }
    }

    private var signUpLink: some View {
        Button {
            onDismiss()
        } label: {
            Text("Create an account")
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextSecondary)
                .underline()
        }
        .frame(height: 44)
        .accessibilityLabel(Text("Create a new account"))
    }
}

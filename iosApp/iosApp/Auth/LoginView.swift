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

                    loginButton
                        .padding(.horizontal, StrakkSpacing.xl)

                    signUpLink
                }
                .padding(.bottom, StrakkSpacing.xxl)
            }
        }
        .alert("Info", isPresented: Binding(
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
            Button {
                onDismiss()
            } label: {
                Image(systemName: "arrow.left")
                    .font(.body.weight(.semibold))
                    .foregroundStyle(Color.strakkTextPrimary)
                    .frame(width: 44, height: 44)
            }
            .accessibilityLabel("Retour")

            Spacer()
        }
        .padding(.horizontal, StrakkSpacing.md)
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            Text("Content de te revoir")
                .font(.strakkHeading1)
                .foregroundStyle(Color.strakkTextPrimary)

            Text("Connecte-toi pour accéder à ton espace.")
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

            TextField("ton@email.com", text: $email)
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
                        .strokeBorder(Color.strakkDivider, lineWidth: 1)
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
                Text("Mot de passe")
                    .font(.strakkCaptionBold)
                    .foregroundStyle(Color.strakkTextSecondary)

                Spacer()

                Button {
                    viewModel.send(LoginEventOnForgotPassword())
                } label: {
                    Text("Mot de passe oublié ?")
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkPrimary)
                }
                .frame(height: 32)
                .accessibilityLabel("Réinitialiser le mot de passe")
            }

            HStack {
                Group {
                    if showPassword {
                        TextField("Ton mot de passe", text: $password)
                    } else {
                        SecureField("Ton mot de passe", text: $password)
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
                .accessibilityLabel(showPassword ? "Masquer le mot de passe" : "Afficher le mot de passe")
            }
            .padding(.horizontal, StrakkSpacing.sm)
            .padding(.vertical, StrakkSpacing.xs)
            .background(Color.strakkSurface1)
            .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
            .overlay(
                RoundedRectangle(cornerRadius: StrakkRadius.sm)
                    .strokeBorder(Color.strakkDivider, lineWidth: 1)
            )
        }
    }

    private var loginButton: some View {
        Button {
            focusedField = nil
            viewModel.send(LoginEventOnLogin())
        } label: {
            Group {
                if viewModel.state.isLoading {
                    ProgressView()
                        .tint(.white)
                } else {
                    Text("Se connecter")
                        .font(.strakkBodyBold)
                        .foregroundStyle(.white)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(viewModel.state.isLoading ? Color.strakkPrimary.opacity(0.6) : Color.strakkPrimary)
            .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
        }
        .disabled(viewModel.state.isLoading)
        .accessibilityLabel("Se connecter")
    }

    private var signUpLink: some View {
        Button {
            onDismiss()
        } label: {
            Text("Créer un compte")
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextSecondary)
                .underline()
        }
        .frame(height: 44)
        .accessibilityLabel("Créer un nouveau compte")
    }
}

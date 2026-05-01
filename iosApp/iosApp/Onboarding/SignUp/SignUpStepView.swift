import SwiftUI
import shared

struct SignUpStepView: View {
    @Bindable var wrapper: OnboardingFlowViewModelWrapper

    @State private var email: String = ""
    @State private var password: String = ""
    @State private var showPassword: Bool = false
    @FocusState private var focusedField: Field?

    private enum Field: Hashable {
        case email, password
    }

    var body: some View {
        VStack(spacing: 0) {
            stepHeader

            ScrollView {
                VStack(spacing: StrakkSpacing.lg) {
                    emailField
                    passwordField
                }
                .padding(.horizontal, StrakkSpacing.xl)
                .padding(.top, StrakkSpacing.xl)
            }

            VStack(spacing: StrakkSpacing.md) {
                if let error = wrapper.state.signUpError {
                    Text(error)
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkError)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, StrakkSpacing.xl)
                        .transition(.opacity)
                }

                createButton
                    .padding(.horizontal, StrakkSpacing.xl)

                loginLink
            }
            .padding(.bottom, StrakkSpacing.xxl)
        }
        .onAppear {
            email = wrapper.state.email
            password = wrapper.state.password
        }
    }

    private var stepHeader: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.sm) {
            OnboardingProgressBar(progress: wrapper.state.progressFraction)
                .padding(.horizontal, StrakkSpacing.xl)

            Text("Crée ton compte")
                .font(.strakkHeading1)
                .foregroundStyle(Color.strakkTextPrimary)
                .padding(.horizontal, StrakkSpacing.xl)
                .padding(.top, StrakkSpacing.xl)
        }
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
                    wrapper.send(OnboardingFlowEventOnEmailChanged(email: newValue))
                }
                .onSubmit {
                    focusedField = .password
                }
        }
    }

    private var passwordField: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            Text("Mot de passe")
                .font(.strakkCaptionBold)
                .foregroundStyle(Color.strakkTextSecondary)

            HStack {
                Group {
                    if showPassword {
                        TextField("Min. 6 caractères", text: $password)
                    } else {
                        SecureField("Min. 6 caractères", text: $password)
                    }
                }
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextPrimary)
                .autocapitalization(.none)
                .autocorrectionDisabled()
                .submitLabel(.done)
                .focused($focusedField, equals: .password)
                .onChange(of: password) { _, newValue in
                    wrapper.send(OnboardingFlowEventOnPasswordChanged(password: newValue))
                }
                .onSubmit {
                    focusedField = nil
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

            Text("Minimum 6 caractères")
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextTertiary)
        }
    }

    private var createButton: some View {
        Button {
            focusedField = nil
            wrapper.send(OnboardingFlowEventOnContinue())
        } label: {
            Group {
                if wrapper.state.isSigningUp {
                    ProgressView()
                        .tint(.white)
                } else {
                    Text("Créer mon compte")
                        .font(.strakkBodyBold)
                        .foregroundStyle(.white)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(wrapper.state.isSigningUp ? Color.strakkPrimary.opacity(0.6) : Color.strakkPrimary)
            .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
        }
        .disabled(wrapper.state.isSigningUp)
        .accessibilityLabel("Créer mon compte")
    }

    private var loginLink: some View {
        Button {
            wrapper.send(OnboardingFlowEventOnNavigateToLogin())
        } label: {
            Text("Se connecter")
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextSecondary)
                .underline()
        }
        .frame(height: 44)
        .accessibilityLabel("Aller à la connexion")
    }
}

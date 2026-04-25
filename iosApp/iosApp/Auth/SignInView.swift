import SwiftUI

struct SignInView: View {
    let initialEmail: String
    let isLoading: Bool
    let error: String?
    let onSignIn: (String, String) -> Void
    let onSwitchToSignUp: () -> Void

    // Local state for instant text input response
    @State private var email: String = ""
    @State private var password: String = ""
    @FocusState private var focusedField: Field?

    private enum Field { case email, password }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Sign in")
                .font(.strakkHeading2)
                .foregroundStyle(Color.strakkTextPrimary)
                .padding(.top, 56)
                .padding(.horizontal, 20)

            Spacer().frame(height: 32)

            VStack(alignment: .leading, spacing: 16) {
                // Email field
                TextField("you@example.com", text: $email)
                    .keyboardType(.emailAddress)
                    .textContentType(.emailAddress)
                    .autocapitalization(.none)
                    .autocorrectionDisabled()
                    .font(.strakkBody)
                    .foregroundStyle(Color.strakkTextPrimary)
                    .tint(Color.strakkPrimary)
                    .focused($focusedField, equals: .email)
                    .frame(height: 48)
                    .padding(.horizontal, 16)
                    .background(Color.strakkSurface1)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(
                                focusedField == .email ? Color.strakkPrimary : Color.strakkDivider,
                                lineWidth: 1
                            )
                    )
                    .accessibilityLabel("Email address")

                // Password field
                SecureField("Password", text: $password)
                    .textContentType(.password)
                    .font(.strakkBody)
                    .foregroundStyle(Color.strakkTextPrimary)
                    .tint(Color.strakkPrimary)
                    .focused($focusedField, equals: .password)
                    .frame(height: 48)
                    .padding(.horizontal, 16)
                    .background(Color.strakkSurface1)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(
                                focusedField == .password ? Color.strakkPrimary : Color.strakkDivider,
                                lineWidth: 1
                            )
                    )
                    .accessibilityLabel("Password")

                if let error {
                    Text(error)
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkError)
                }
            }
            .padding(.horizontal, 20)

            Spacer()

            VStack(spacing: 16) {
                // Sign in button
                Button {
                    onSignIn(email, password)
                } label: {
                    ZStack {
                        if isLoading {
                            ProgressView()
                                .tint(.white)
                        } else {
                            Text("Sign In")
                                .font(.strakkBodyBold)
                                .foregroundStyle(.white)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(Color.strakkPrimary)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .disabled(isLoading || email.isEmpty || password.isEmpty)
                .opacity(isLoading || email.isEmpty || password.isEmpty ? 0.5 : 1.0)
                .accessibilityLabel("Sign in")

                // Switch to sign up
                Button(action: onSwitchToSignUp) {
                    Text("Don't have an account? ")
                        .font(.strakkBody)
                        .foregroundStyle(Color.strakkTextSecondary)
                    + Text("Sign Up")
                        .font(.strakkBody)
                        .foregroundStyle(Color.strakkPrimary)
                }
                .accessibilityLabel("Create an account")
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 48)
        }
        .onAppear {
            email = initialEmail
            focusedField = .email
        }
    }
}

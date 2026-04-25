import SwiftUI

struct SignUpView: View {
    let initialEmail: String
    let isLoading: Bool
    let error: String?
    let onSignUp: (String, String) -> Void
    let onSwitchToSignIn: () -> Void

    // Local state for instant text input response
    @State private var email: String = ""
    @State private var password: String = ""
    @FocusState private var focusedField: Field?

    private enum Field { case email, password }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Create account")
                .font(.strakkHeading2)
                .foregroundStyle(Color.strakkTextPrimary)
                .padding(.top, 56)
                .padding(.horizontal, 20)

            Spacer().frame(height: 8)

            Text("Start tracking your fitness journey")
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextSecondary)
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
                    .textContentType(.newPassword)
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
                // Sign up button
                Button {
                    onSignUp(email, password)
                } label: {
                    ZStack {
                        if isLoading {
                            ProgressView()
                                .tint(.white)
                        } else {
                            Text("Sign Up")
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
                .accessibilityLabel("Create account")

                // Switch to sign in
                Button(action: onSwitchToSignIn) {
                    Text("Already have an account? ")
                        .font(.strakkBody)
                        .foregroundStyle(Color.strakkTextSecondary)
                    + Text("Sign In")
                        .font(.strakkBody)
                        .foregroundStyle(Color.strakkPrimary)
                }
                .accessibilityLabel("Sign in to existing account")
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

import SwiftUI
import shared

@MainActor
@Observable
final class GreetingViewModelWrapper {
    private let viewModel: GreetingViewModel

    var greeting: String = ""
    var platform: String = ""
    var isLoading: Bool = true
    var errorMessage: String?

    init() {
        self.viewModel = KoinHelper().getGreetingViewModel()
    }

    func load() async {
        isLoading = true
        errorMessage = nil
        do {
            let result = try await viewModel.loadGreeting()
            greeting = result.message
            platform = result.platform
            isLoading = false
        } catch {
            errorMessage = error.localizedDescription
            isLoading = false
        }
    }
}

@MainActor
@Observable
final class QuoteViewModelWrapper {
    private let viewModel: QuoteViewModel

    var quoteText: String = ""
    var quoteAuthor: String = ""
    var isLoading: Bool = true
    var errorMessage: String?

    init() {
        self.viewModel = KoinHelper().getQuoteViewModel()
    }

    func load() async {
        isLoading = true
        errorMessage = nil
        do {
            let result = try await viewModel.fetchQuote()
            quoteText = result.text
            quoteAuthor = result.author
            isLoading = false
        } catch {
            errorMessage = error.localizedDescription
            isLoading = false
        }
    }
}

struct ContentView: View {
    @State private var greetingWrapper = GreetingViewModelWrapper()
    @State private var quoteWrapper = QuoteViewModelWrapper()

    var body: some View {
        VStack(spacing: 24) {
            // Greeting section
            Group {
                if greetingWrapper.isLoading {
                    ProgressView()
                } else if let error = greetingWrapper.errorMessage {
                    Text(error)
                        .foregroundStyle(.red)
                } else {
                    VStack(spacing: 12) {
                        Text(greetingWrapper.greeting)
                            .font(.title)
                            .fontWeight(.semibold)
                        Text("Running on: \(greetingWrapper.platform)")
                            .font(.body)
                            .foregroundStyle(.secondary)
                    }
                }
            }

            Divider()

            // Quote section
            Group {
                if quoteWrapper.isLoading {
                    ProgressView()
                } else if let error = quoteWrapper.errorMessage {
                    Text(error)
                        .foregroundStyle(.red)
                } else {
                    VStack(spacing: 12) {
                        Text("\"\(quoteWrapper.quoteText)\"")
                            .font(.body)
                            .multilineTextAlignment(.center)
                        Text("— \(quoteWrapper.quoteAuthor)")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                }
            }

            Button("New Quote") {
                Task {
                    await quoteWrapper.load()
                }
            }
            .buttonStyle(.borderedProminent)
        }
        .padding()
        .task {
            await greetingWrapper.load()
        }
        .task {
            await quoteWrapper.load()
        }
    }
}

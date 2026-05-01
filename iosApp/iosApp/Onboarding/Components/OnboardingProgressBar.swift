import SwiftUI

struct OnboardingProgressBar: View {
    let progress: Float

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule()
                    .fill(Color.strakkSurface2)
                    .frame(height: 4)

                Capsule()
                    .fill(Color.strakkPrimary)
                    .frame(width: geo.size.width * CGFloat(max(0, min(1, progress))), height: 4)
                    .animation(.easeInOut(duration: 0.3), value: progress)
            }
        }
        .frame(height: 4)
    }
}

import { createContext, useContext } from 'react'

// A DOM node mounted at the shell root — OUTSIDE any momentum-scroll
// (`-webkit-overflow-scrolling: touch`) or transformed container. Full-screen
// pushed pages portal into it so iOS WebKit anchors their `position: fixed`
// layer to the viewport instead of trapping it inside the scrolling screen
// (which left the tab bar peeking under the page). On the desktop device frame
// the host still sits inside the scaled frame, so pages cover the frame, not
// the whole window.
export const OverlayContext = createContext<HTMLElement | null>(null)

export const useOverlayHost = () => useContext(OverlayContext)

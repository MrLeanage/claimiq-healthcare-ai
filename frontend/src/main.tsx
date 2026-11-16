import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './app/App';
import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import { DraftGuardProvider } from './app/DraftGuardProvider';
import '@fontsource/roboto/400.css';
import '@fontsource/roboto/600.css';
import '@fontsource/roboto/700.css';
import '@fontsource/open-sans/400.css';
import '@fontsource/open-sans/600.css';
import '@fontsource/open-sans/700.css';
import './styles/main.css';
// Entry point: the HTML root hosts React; routing wraps App and the unsaved-draft guard.
const router = createBrowserRouter([
  {
    path: '*',
    element: (
      <DraftGuardProvider>
        <App />
      </DraftGuardProvider>
    ),
  },
]);
// JSX describes components; React renders them into the existing DOM container.
createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <RouterProvider router={router} />
  </StrictMode>,
);

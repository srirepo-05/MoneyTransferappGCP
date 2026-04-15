import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

/**
 * Wraps protected routes. Redirects unauthenticated users to /login
 * while preserving the originally-requested location for post-login redirect.
 */
export default function PrivateRoute({ children }) {
  const { isAuthenticated } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }
  return children;
}

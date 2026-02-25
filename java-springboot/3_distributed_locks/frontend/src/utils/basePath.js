export function getBasePath() {
  const defaultBase = process.env.BASE_URL || '/';
  const pathname = window.location.pathname || '';
  const match = pathname.match(/^\/workshop\/distributed-locks/);
  if (match) {
    return match[0].replace(/\/$/, '');
  }
  return defaultBase.replace(/\/$/, '');
}

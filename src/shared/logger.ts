type Level = 'info' | 'warn' | 'error';

function emit(level: Level, scope: string, message: string, extra?: unknown): void {
  const ts = new Date().toISOString();
  const line = `${ts} [${level.toUpperCase()}] [${scope}] ${message}`;
  const args = extra === undefined ? [line] : [line, extra];
  if (level === 'error') console.error(...args);
  else if (level === 'warn') console.warn(...args);
  else console.log(...args);
}

export const logger = {
  info: (scope: string, message: string, extra?: unknown) => emit('info', scope, message, extra),
  warn: (scope: string, message: string, extra?: unknown) => emit('warn', scope, message, extra),
  error: (scope: string, message: string, extra?: unknown) => emit('error', scope, message, extra),
};

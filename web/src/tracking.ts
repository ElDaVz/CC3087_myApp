export type TrackingSummary = {submissions:number; pending:number; inProgress:number; completed:number};

/** The tracking URL carries a secret token but never form answers or identity data. */
export function trackingPath(pathname:string):string|null {
 const parts=pathname.split('/').filter(Boolean);
 return parts.length===2&&parts[0]==='t'&&/^[A-Za-z0-9_-]{20,64}$/.test(parts[1])?parts[1]:null;
}

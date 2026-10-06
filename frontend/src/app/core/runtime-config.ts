export interface MuseumRuntimeConfig { apiBaseUrl?: string; publicBaseUrl?: string }
function config():MuseumRuntimeConfig { return (globalThis as typeof globalThis & { __MUSEUM_CONFIG__?:MuseumRuntimeConfig }).__MUSEUM_CONFIG__ || {}; }
function normalize(value:string):string { return value.replace(/\/+$/,''); }
export function apiBaseUrl():string { return normalize(config().apiBaseUrl || '/api'); }
export function publicBaseUrl():string { return normalize(config().publicBaseUrl || window.location.origin); }
export function hasPublicBaseUrl():boolean { return !!config().publicBaseUrl?.trim(); }
export function apiUrl(path:string):string { return `${apiBaseUrl()}${path.startsWith('/')?path:`/${path}`}`; }

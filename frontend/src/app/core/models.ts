// Fields mirror the live /v3/api-docs and backend response records.
export interface Account { id:number; email:string; active:boolean; roles:string[]; registeredAt:string; lastLogin:string|null }
export interface Token { accessToken:string; tokenType:string; expiresIn:number }
export interface Visitor { id:number; accountId:number; firstName:string; lastName:string; phone:string|null; recommendationsConsent:boolean }
export interface Registration { email:string; password:string; firstName:string; lastName:string; phone:string; recommendationsConsent:boolean }
export interface Museum { id:number; name:string; foundationYear:number|null; address:string; description:string|null; active:boolean }
export interface Hall { id:number; museumId:number; name:string; capacity:number }
export interface Term { id:number; exhibitionId:number; start:string; end:string; capacity:number; status:string }
export interface Exhibition { id:number; hallId:number; name:string; description:string|null; start:string; end:string; status:string; onlineVisible:boolean; terms:Term[] }
export interface Price { exhibitionId:number; validFrom:string; validTo:string|null; category:string; price:number }
export interface Ticket { id:number; visitorId:number|null; termId:number; exhibitionId:number; category:string; purchasedAt:string; paidPrice:number; status:string; qrToken:string; validUntil:string }
export interface Visit { id:number; ticketId:number; enteredAt:string; exitedAt:string|null; status:string }
export interface Recommendation { participationId:number; exhibitId:number; name:string; explanation:string }
export interface Scan { id:number; visitId:number; participationId:number; scannedAt:string; durationSeconds:number|null; next:Recommendation[]; participation:Participation }
export type Criterion = 'KOMBINACIJA'|'INTERESOVANJA'|'BLIZINA'|'POPULARNOST';
export interface RouteItem { position:number; participationId:number; explanation:string; visited:boolean }
export interface VisitRoute { id:number; visitId:number; generatedAt:string; criterion:Criterion; status:string; items:RouteItem[] }
export interface ExhibitionRecommendation { exhibitionId:number; name:string; start:string; end:string; explanation:string }
export interface Participation { participationId:number; name:string; contextDescription:string|null; content:{id:number; type:string; path:string; language:string; version:number}[] }

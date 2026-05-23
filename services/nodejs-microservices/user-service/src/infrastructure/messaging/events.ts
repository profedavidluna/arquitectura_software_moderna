/**
 * User Domain Events
 * 
 * Events represent facts that have occurred in the system.
 * They are immutable and named in past tense (UserCreated, not CreateUser).
 * Other services can react to these events without direct coupling.
 */
export const USER_EVENTS_TOPIC = 'user-events';

export enum UserEventType {
  USER_CREATED = 'USER_CREATED',
  USER_UPDATED = 'USER_UPDATED',
  USER_DELETED = 'USER_DELETED',
  ADDRESS_ADDED = 'ADDRESS_ADDED',
}

export interface UserEvent {
  eventType: UserEventType;
  timestamp: string;
  source: string;
  data: any;
}

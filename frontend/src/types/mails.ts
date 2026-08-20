import { Attachment } from './attachment';
import { User } from './user';

enum MailStatus {
  DRAFT = 'DRAFT',
  SENT = 'SENT',
  ERROR = 'ERROR',
}

enum MailSource{
  INTERN = 'INTERN',
  EXTERN = 'EXTERN',
}

export type Mail = {
  id: string;
  sender: User;
  subject: string;
  content: string;
  status: MailStatus;
  source: MailSource;
  trackingCode?: string;
  externalSenderEmail?: string;
  inReplyToMailId?: string;
  to: User[];
  cc: User[];
  bcc: User[];
  attachments: Attachment[];
  createdAt: string;
  updatedAt: string;
  sentAt?: string;
};

export type MailListItem = Omit<Mail, 'to' | 'cc' | 'bcc' | 'attachments'> & {
  attachmentCount: number;
};

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
};

export type CreateMail = {
  subject: string;
  content: string;
  toIds: string[];
  ccIds: string[];
  bccIds: string[];
};

export type UpdateMail = CreateMail;

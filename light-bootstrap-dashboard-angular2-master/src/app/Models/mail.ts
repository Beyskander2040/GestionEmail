import { Attachment } from "./attachment";

export interface Mail {
    id: number;
    subject: string;
    sender: string;
    content: string;
    receivedDate: Date;
    attachments: Attachment[];
  }
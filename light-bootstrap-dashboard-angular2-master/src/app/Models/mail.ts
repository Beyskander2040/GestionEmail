import { Attachment } from "./attachment";

export interface Mail {
    id: number;
    uid :String;
    subject: string;
    sender: string;
    content: string;
    receivedDate: Date;
    attachments: Attachment[];
  }
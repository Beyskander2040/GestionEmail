import { Component, OnInit, ViewChild } from '@angular/core';
import { MatPaginator } from '@angular/material/paginator';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { Mail } from 'app/Models/mail';
import { EmailService } from 'app/Services/email.service';

@Component({
  selector: 'app-mail',
  templateUrl: './mail.component.html',
  styleUrls: ['./mail.component.scss']
})
export class MailComponent implements OnInit {
  emails: Mail[] = [];
  displayedColumns: string[] = ['subject', 'from', 'receivedDate', 'content', 'attachments'];
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  loading: boolean = false;

  constructor(
    private emailService: EmailService,
    private sanitizer: DomSanitizer,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      const mailboxId = params['mailboxId'];
      if (mailboxId) {
        this.loadAllEmails(mailboxId);
      }
    });
  }

  loadAllEmails(mailboxId: number): void {
    this.loading = true;
    this.emailService.getEmailsForMailbox(mailboxId).subscribe(
      (data: Mail[]) => {
        console.log('Fetched Emails with Attachments:', data); // Check if attachments are present

        this.emails = data.map(email => ({
          ...email,
          safeContent: this.sanitizer.bypassSecurityTrustHtml(email.content),
          fullContentVisible: false
        })).sort((a, b) => new Date(b.receivedDate).getTime() - new Date(a.receivedDate).getTime());

        this.loading = false;
      },
      (error) => {
        console.error('Error fetching emails:', error);
        this.loading = false;
      }
    );
}


  getAttachmentUrl(attachment: any): string {
    return `path_to_attachments/${attachment.filename}`;
  }
}

import { Component, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { Mail } from 'app/Models/mail';
import { EmailService } from 'app/Services/email.service';


@Component({
  selector: 'app-mail',
  templateUrl: './mail.component.html',
  styleUrls: ['./mail.component.scss']
})
export class MailComponent implements OnInit {
  emails: { mail: Mail, safeContent: SafeHtml, fullContentVisible: boolean }[] = [];
  page: number = 0;
  size: number = 10;
  loading: boolean = false;
  allEmailsLoaded: boolean = false;

  constructor(private emailService: EmailService, private sanitizer: DomSanitizer) { }

  ngOnInit(): void {
    this.loadEmails(); // Initial load of emails
  }

  loadEmails(): void {
    this.loading = true;
    const offset = this.page * this.size; // Calculate offset based on page number
    this.emailService.getEmails(offset, this.size).subscribe(
      (data: Mail[]) => {
        if (data.length < this.size) {
          this.allEmailsLoaded = true; // All emails have been loaded
        }
        const emailsWithSafeContent = data.map(email => ({
          mail: email,
          safeContent: this.sanitizer.bypassSecurityTrustHtml(email.content),
          fullContentVisible: false // Initially hide full content
        }));
        this.emails = [...this.emails, ...emailsWithSafeContent];
        this.loading = false;
      },
      (error) => {
        console.error('Error fetching emails:', error);
        this.loading = false;
      }
    );
  }

  toggleFullContent(emailData: { mail: Mail, safeContent: SafeHtml, fullContentVisible: boolean }): void {
    emailData.fullContentVisible = !emailData.fullContentVisible;
  }

  getAttachmentUrl(attachment: any): string {
    // Adjust this method to generate the correct URL for downloading the attachment
    return `path_to_attachments/${attachment.filename}`;
  }

  loadMore(): void {
    this.page++;
    this.loadEmails();
  }
}

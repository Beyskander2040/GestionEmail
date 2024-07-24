import { Component, OnInit } from '@angular/core';
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
  emails: { mail: Mail, safeContent: SafeHtml, fullContentVisible: boolean }[] = [];
  page: number = 0;
  size: number = 10;
  loading: boolean = false;
  allEmailsLoaded: boolean = false;
  email: string = ''; // Define email property
  password: string = ''; // Define password property

  constructor(
    private emailService: EmailService,
    private sanitizer: DomSanitizer,
    private route: ActivatedRoute
  ) { }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      const emailData = params['emailData'];
      if (emailData) {
        this.emails = JSON.parse(emailData);
        this.emails.forEach(emailData => {
          emailData.safeContent = this.sanitizer.bypassSecurityTrustHtml(emailData.mail.content);
          emailData.fullContentVisible = false;
        });
      }
      this.email = params['email'] || '';
      this.password = params['password'] || '';
      this.page = parseInt(params['page'], 10) || 0;
      this.size = parseInt(params['size'], 10) || 10;
    });
  }

  loadEmails(): void {
    this.loading = true;
    this.emailService.getEmails(
      this.email,
      this.password,
      this.page,
      this.size
    ).subscribe(
      (data: Mail[]) => {
        if (data.length < this.size) {
          this.allEmailsLoaded = true;
        }
        const emailsWithSafeContent = data.map(email => ({
          mail: email,
          safeContent: this.sanitizer.bypassSecurityTrustHtml(email.content),
          fullContentVisible: false
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
    return `path_to_attachments/${attachment.filename}`;
  }

  loadMore(): void {
    if (!this.allEmailsLoaded && !this.loading) {
      this.page++;
      this.loadEmails();
    }
  }
}

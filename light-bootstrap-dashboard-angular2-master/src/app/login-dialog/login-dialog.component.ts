import { Component, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { DomSanitizer } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { Mail } from 'app/Models/mail';
import { EmailService } from 'app/Services/email.service';
import { MailBoxService } from 'app/Services/mail-box.service';

@Component({
  selector: 'app-login-dialog',
  templateUrl: './login-dialog.component.html',
  styleUrls: ['./login-dialog.component.scss']
})
export class LoginDialogComponent {
  email: string = '';
  password: string = '';
  loading: boolean = false;
  emails: any[] = [];
  page: number = 0;
  size: number = 10;

  constructor(
    public dialogRef: MatDialogRef<LoginDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
    private emailService: EmailService,
    private sanitizer: DomSanitizer,
    private router: Router,
    private mailboxService: MailBoxService, // inject the service

  ) {
    this.email = data.emailAddress;
    this.password = data.password;
  }

  onNoClick(): void {
    this.dialogRef.close();
  }

  onFetchEmails(mailboxId: number): void {
    this.loading = true;
    
    console.log('Starting new emails check...');
    this.emailService.checkNewEmails(this.email, this.password, mailboxId).subscribe(
      () => {
        console.log('New emails check succeeded.');
        console.log('Fetching all emails...');
        this.emailService.getEmailsForMailbox(mailboxId).subscribe(
          (data: Mail[]) => {
            console.log('Emails fetched:', data);
            const emailsWithSafeContent = data.map(email => ({
              mail: email,
              safeContent: this.sanitizer.bypassSecurityTrustHtml(email.content),
              fullContentVisible: false
            }));
            this.emails = [...this.emails, ...emailsWithSafeContent];
            this.loading = false;
            this.mailboxService.setMailboxId(mailboxId); // Save the mailboxId
           this.dialogRef.close({
              email: this.email,
              password: this.password,
              emails: emailsWithSafeContent,
              mailboxId: mailboxId
            });
          },
          (error) => {
            console.error('Error fetching emails:', error);
            this.loading = false;
          }
        );
      },
      (error) => {
        console.error('Error checking new emails:', error);
        this.loading = false;
      }
    );
  }
  
  
}
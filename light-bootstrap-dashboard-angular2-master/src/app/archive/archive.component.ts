import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { EmailService } from 'app/Services/email.service';
import { MailBoxService } from 'app/Services/mail-box.service';

@Component({
  selector: 'app-archive',
  templateUrl: './archive.component.html',
  styleUrls: ['./archive.component.scss']
})
export class ArchiveComponent implements OnInit {
  emails: any[] = [];
  mailboxId: number;

  constructor( private emailService: EmailService,
    private route: ActivatedRoute,
    private mailboxService: MailBoxService // inject the service
  ) { }


    ngOnInit(): void {
      this.mailboxId = this.mailboxService.getMailboxId();
      if (this.mailboxId !== null) {
        this.fetchArchivedEmails(this.mailboxId);
      } else {
        console.error('Mailbox ID is not set.');
      }
    }
    fetchArchivedEmails(mailboxId: number): void {
      this.emailService.getArchivedEmails(mailboxId).subscribe((data: any[]) => {
        this.emails = data;
      });
    }
}

import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { catchError, fromEvent, Observable, Subject, throwError } from 'rxjs';
import { AuthGuard } from './auth.guard';


@Injectable({
  providedIn: 'root'
})
export class MailBoxService {
  private apiUrl = 'http://localhost:8088/MailBox/api/v1/auth/mailboxes';
  private apiUrl1 = 'http://localhost:8083/email/api/v1/emails';
  private progressSubject = new Subject<number>();



  constructor(private http: HttpClient, private authguard : AuthGuard) { }
   // Fetch mailboxes by user ID
   getMailboxes(userId: number): Observable<any> {
    const token = localStorage.getItem('authToken');
    console.log('Token from localStorage:', localStorage.getItem('authToken'));

    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  
    return this.http.get(`${this.apiUrl}/getMailBoxByuser`, { headers, params: { userId: userId.toString() } })
      .pipe(
        catchError(err => {
          console.error('Error fetching mailboxes', err);
          return throwError(err);
        })
      );
  }
  createMailbox(userEmail: string, userPassword: string): Observable<any> {
    const token = localStorage.getItem('authToken');
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/x-www-form-urlencoded'
    });

    const body = new URLSearchParams();
    body.set('userEmail', userEmail);
    body.set('userPassword', userPassword);

    return this.http.post(`${this.apiUrl}/createWithProgress`, body.toString(), { headers, observe: 'response' });
  }
  

  deleteMailbox(mailboxId: number): Observable<any> {
    const token = localStorage.getItem('authToken');
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });

    return this.http.delete(`${this.apiUrl}/delete/${mailboxId}`, { headers })
      .pipe(
        catchError(err => {
          console.error('Error deleting mailbox', err);
          return throwError(err);
        })
      );
  }

  getProgress(mailboxId: number): Observable<number> {
    const eventSource = new EventSource(`http://localhost:8083/email/api/v1/emails/progress/${mailboxId}`);
    eventSource.addEventListener('progress', (event: any) => {
      this.progressSubject.next(JSON.parse(event.data));
    });
    return this.progressSubject.asObservable();
  }
  
}

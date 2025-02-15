import { HttpClient, HttpErrorResponse, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Mail } from 'app/Models/mail';
import { Page } from 'app/Models/page';
import { catchError, Observable, tap, throwError } from 'rxjs';
import { map } from 'rxjs/operators';


@Injectable({
  providedIn: 'root'
})
export class EmailService {
  private apiUrl = 'http://localhost:8088/email/api/v1/emails';

  constructor(private http: HttpClient) { }
 
  getEmails(userEmail: string, userPassword: string, page: number, size: number): Observable<Mail[]> {
    const url = `${this.apiUrl}/emails?email=${userEmail}&password=${userPassword}&page=${page}&size=${size}`;
    const token = localStorage.getItem('authToken');
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
    console.log('Request URL:', url); // Debugging
    console.log('Headers:', headers); // Debugging
    return this.http.get<Mail[]>(url, { headers });
  }

  readEmails(): Observable<string> {
    const url = `${this.apiUrl}/read`;
    return this.http.get<string>(url);
  }
  fetchEmails(mailboxId: number, email: string, password: string): Observable<any> {
    const token = localStorage.getItem('authToken');

    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/x-www-form-urlencoded' // Set this to x-www-form-urlencoded for query parameters
    });

    const params = new URLSearchParams();
    params.set('mailboxId', mailboxId.toString());
    params.set('email', email);
    params.set('password', password);

    return this.http.post(`${this.apiUrl}/fetch`, params.toString(), { headers, responseType: 'text' })
      .pipe(
        catchError(err => {
          console.error('Error fetching emails', err);
          return throwError(err);
        })
      );
  }
  checkNewEmails(email: string, password: string, mailboxId: number): Observable<any> {
    const url = `${this.apiUrl}/check?email=${encodeURIComponent(email)}&password=${encodeURIComponent(password)}&mailboxId=${mailboxId}`;
    const token = localStorage.getItem('authToken');
  
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  
    return this.http.post(url, null, { headers, responseType: 'text' }) 
      .pipe(
        map(response => {
          return response;
        }),
        catchError(err => {
          console.error('Error checking new emails', err);
          return throwError(err);
        })
      );
  }
   getEmailsForMailbox(mailboxId: number): Observable<Mail[]> {
  const url = `${this.apiUrl}/all?mailboxId=${mailboxId}`;
  const token = localStorage.getItem('authToken');
  const headers = new HttpHeaders({
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  });

  return this.http.get<Mail[]>(url, { headers })
    .pipe(
      catchError(err => {
        console.error('Error fetching emails for mailbox', err);
        return throwError(err);
      })
    );
}
archiveEmail(mailUid: string, mailboxId: number): Observable<any> {
  const url = `${this.apiUrl}/archive`;
  const params = new HttpParams()
  .set('mailUid', encodeURIComponent(mailUid)) // Ensure UUID is URL-encoded
  .set('mailboxId', mailboxId.toString());

  const headers = new HttpHeaders({
    'Authorization': `Bearer ${localStorage.getItem('authToken')}`
  });

  return this.http.post(url, null, { headers, params });
}

getArchivedEmails(mailboxId: number): Observable<any[]> {
  const token = localStorage.getItem('authToken');
  const headers = new HttpHeaders({
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  });  return this.http.get<any[]>(`${this.apiUrl}/archived-emails?mailboxId=${mailboxId}`, { headers });
}

deleteEmail(mailUid: string, emailId: number, mailboxId: number): Observable<any> {
  const url = `${this.apiUrl}/delete`;
  const params = new HttpParams()
    .set('mailUid', encodeURIComponent(mailUid)) // Ensure UID is URL-encoded
    .set('emailId', emailId.toString())
    .set('mailboxId', mailboxId.toString());

  const token = localStorage.getItem('authToken');
  const headers = new HttpHeaders({
    'Authorization': `Bearer ${token}`
  });

  return this.http.delete(url, { headers, params }).pipe(
    catchError(error => {
      console.error('Error deleting email:', error);
      return throwError(error);
    })
  );
}
getTopDomainsWithAttachments(): Observable<any> {
  const token = localStorage.getItem('authToken');
  const headers = new HttpHeaders({
    'Authorization': `Bearer ${token}`
  });
  return this.http.get(`${this.apiUrl}/top-domains-attachments`, { headers });
}



}
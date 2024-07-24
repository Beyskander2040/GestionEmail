import { Routes } from '@angular/router';

import { UserComponent } from '../../user/user.component';
import { MailComponent } from 'app/mail/mail.component';
import { ListMailBoxComponent } from 'app/list-mail-box/list-mail-box.component';



export const AdminLayoutRoutes: Routes = [
    { path: 'user',component: UserComponent },
    { path: 'Mail', component: MailComponent },
    { path: 'MesBoites', component: ListMailBoxComponent },
   
];

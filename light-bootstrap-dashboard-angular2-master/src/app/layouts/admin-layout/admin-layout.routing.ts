import { Routes } from '@angular/router';

import { UserComponent } from '../../user/user.component';
import { MailComponent } from 'app/mail/mail.component';
import { ListMailBoxComponent } from 'app/list-mail-box/list-mail-box.component';
import { ArchiveComponent } from 'app/archive/archive.component';
import { TopDomainsChartComponent } from 'app/top-domains-chart/top-domains-chart.component';



export const AdminLayoutRoutes: Routes = [
    { path: 'user',component: UserComponent },
    { path: 'Mail', component: MailComponent },
    { path: 'MesBoites', component: ListMailBoxComponent },
    { path: 'archive/:mailboxId', component: ArchiveComponent } ,
    { path: 'statistiques', component: TopDomainsChartComponent }  

   
];

import { TestBed } from '@angular/core/testing';

import { MailBoxService } from './mail-box.service';

describe('MailBoxService', () => {
  let service: MailBoxService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(MailBoxService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

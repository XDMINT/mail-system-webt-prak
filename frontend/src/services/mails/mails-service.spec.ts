import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { MailsService } from './mails-service';
import { API_BASE_URL } from '../../constants';

describe('MailsService', () => {
  let service: MailsService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(MailsService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should create a reply draft for the selected incoming mail', () => {
    service.createReplyDraft('incoming-id').subscribe();

    const request = httpTesting.expectOne(`${API_BASE_URL}/mails/incoming-id/reply`);
    expect(request.request.method).toBe('POST');
    request.flush({ id: 'reply-id' });
  });
});

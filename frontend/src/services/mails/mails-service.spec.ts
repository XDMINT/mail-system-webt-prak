import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { MailsService } from './mails-service';
import { API_BASE_URL } from '../../constants';
import { CreateMail } from '../../types/mails';

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

  it('should omit the attachment part when a mail has no files', () => {
    const mail: CreateMail = {
      subject: 'Subject',
      content: 'Content',
      toIds: ['receiver-id'],
      ccIds: [],
      bccIds: [],
    };

    service.createDraft(mail, []).subscribe();

    const request = httpTesting.expectOne(`${API_BASE_URL}/mails`);
    const body = request.request.body as FormData;
    expect(request.request.method).toBe('POST');
    expect(body.has('data')).toBe(true);
    expect(body.has('attachments')).toBe(false);
    request.flush({ id: 'draft-id' });
  });

  it('should explicitly request the protected preview representation', () => {
    service.fetchAttachment('attachment-id', true).subscribe();

    const request = httpTesting.expectOne(
      (candidate) => candidate.url === `${API_BASE_URL}/attachments/attachment-id`,
    );
    expect(request.request.params.get('preview')).toBe('true');
    request.flush(new Blob());
  });
});

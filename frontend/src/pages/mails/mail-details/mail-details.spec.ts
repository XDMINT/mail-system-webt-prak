import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MessageService } from 'primeng/api';

import { MailDetails } from './mail-details';

describe('MailDetails', () => {
  let component: MailDetails;
  let fixture: ComponentFixture<MailDetails>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MailDetails],
      providers: [MessageService, provideRouter([])],
    })
    .compileComponents();

    fixture = TestBed.createComponent(MailDetails);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should only preview safe raster image types', () => {
    const attachment = { id: 'attachment-id', fileName: 'image', size: 1 };

    expect(component.isPreviewable({ ...attachment, mimeType: 'image/png' })).toBe(true);
    expect(component.isPreviewable({ ...attachment, mimeType: 'image/svg+xml' })).toBe(false);
    expect(component.isPreviewable({ ...attachment, mimeType: 'text/html' })).toBe(false);
  });
});

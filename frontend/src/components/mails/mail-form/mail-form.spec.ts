import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MessageService } from 'primeng/api';

import { MailForm } from './mail-form';

describe('MailForm', () => {
  let component: MailForm;
  let fixture: ComponentFixture<MailForm>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MailForm],
      providers: [MessageService, provideRouter([])],
    })
    .compileComponents();

    fixture = TestBed.createComponent(MailForm);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should retain only files accepted by the upload component', () => {
    const rejectedFile = new File([new Uint8Array(6 * 1024 * 1024)], 'too-large.bin');
    const acceptedFile = new File([new Uint8Array([1, 2, 3])], 'accepted.png', { type: 'image/png' });

    component.onFileSelect({
      originalEvent: new Event('change'),
      files: [rejectedFile],
      currentFiles: [],
    });
    component.onFileSelect({
      originalEvent: new Event('change'),
      files: [acceptedFile],
      currentFiles: [acceptedFile],
    });

    expect(component['buildAttachmentData']()).toEqual([acceptedFile]);
  });
});

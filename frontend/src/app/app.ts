import {ApplicationConfig, Component, signal} from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { provideNgOpenapi } from './providers';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('frontend');
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideNgOpenapi({ basePath: 'http://localhost/api/v1' })
  ]
};

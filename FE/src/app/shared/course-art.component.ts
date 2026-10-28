import { Component, input } from '@angular/core';

@Component({
  selector: 'app-course-art',
  host: { '[class]': '"course-art art-" + theme()' },
  template: `
    <div class="art-grid" aria-hidden="true"></div>
    <span class="art-label">{{ labels[theme()] || 'KHÓA HỌC' }}</span>
    @switch (theme()) {
      @case ('angular') {
        <div class="angular-shield" aria-hidden="true">A</div>
        <div class="art-code" aria-hidden="true">&lt; build your future /&gt;</div>
      }
      @case ('figma') {
        <div class="figma-mark" aria-hidden="true">
          <i></i>
          <i></i>
          <i></i>
          <i></i>
          <i></i>
        </div>
        <span class="art-caption" aria-hidden="true">
          make it
          <br />
          <em>meaningful.</em>
        </span>
      }
      @case ('java') {
        <div class="java-mark" aria-hidden="true">
          &#123;
          <span>J</span>
          &#125;
        </div>
        <div class="art-code" aria-hidden="true">ideas → powerful APIs</div>
      }
      @case ('python') {
        <div class="python-mark" aria-hidden="true">
          <span>py</span>
          <span>thon.</span>
        </div>
        <span class="art-sticker" aria-hidden="true">Hello, world ↗</span>
      }
      @case ('marketing') {
        <div class="marketing-bars" aria-hidden="true">
          <i></i>
          <i></i>
          <i></i>
          <span>↗</span>
        </div>
        <span class="art-caption" aria-hidden="true">
          think big.
          <br />
          <em>grow bigger.</em>
        </span>
      }
      @case ('illustrator') {
        <div class="illustrator-mark" aria-hidden="true">
          Ai
          <span>✳</span>
        </div>
        <div class="art-code" aria-hidden="true">let your ideas bloom.</div>
      }
    }
    <span class="art-corner" aria-hidden="true">↗</span>
  `,
})
export class CourseArtComponent {
  readonly theme = input('angular');
  readonly labels: Record<string, string> = {
    angular: 'CODE YOUR NEXT CHAPTER',
    figma: 'DESIGN WITH PURPOSE',
    java: 'BUILD SOMETHING GREAT',
    python: 'YOUR FIRST LINE OF CODE',
    marketing: 'IDEAS THAT CONNECT',
    illustrator: 'MADE TO CREATE',
  };
}

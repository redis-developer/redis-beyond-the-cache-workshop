export const markdownContentFixture = {
  viewId: 'shared-markdown-context-fixture',
  route: '/fixture',
  pageType: 'narrative',
  title: 'Markdown content context fixture',
  summary: 'Fixture for markdown first content.',
  sections: [
    {
      sectionId: 'main',
      title: 'Runtime context',
      markdown: [
        'Session `{{ session.id }}` can open the [learner app]({{ links.learnerApp }}).',
        'Runtime status is `{{ runtime.status }}`.',
        '',
        '::widget{id="shared.session-status"}',
        '',
        'Redis Insight link should be validated: [Redis Insight]({{ links.redisInsight }}).',
        'Unsupported placeholder remains visible: `{{ links.adminPanel }}`.',
        'Missing placeholder remains visible: `{{ status.detail }}`.'
      ].join('\n')
    }
  ]
};

export const markdownContextFixture = {
  session: {
    id: 'session-123<script>'
  },
  links: {
    learnerApp: 'https://learner.example.test/app/session-123',
    redisInsight: 'javascript:alert(1)'
  },
  runtime: {
    status: 'running'
  },
  status: {}
};

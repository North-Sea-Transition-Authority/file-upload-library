const sass = require('gulp-sass')(require('sass'));
const postcss = require('gulp-postcss');
const gulp = require('gulp');
const sourcemaps = require('gulp-sourcemaps');
const autoprefixer = require('autoprefixer');
const rename = require("gulp-rename");

const sassOptions = {
  outputStyle: 'compressed',
  includePath: 'src/main/resources/scss'
};

function compileSass(exitOnError) {
  const plugins = [
    autoprefixer({
      browsers: ['last 3 versions', '> 0.1%', 'Firefox ESR'],
      grid: true
    })
  ];

  let sassTask = sass(sassOptions);
  if (!exitOnError) {
    //Without an error handler specified, the task will exit on error, which we want for the "buildAll" task
    sassTask = sassTask.on('error', sass.logError);
  }

  return gulp.src('src/main/resources/scss/*.scss', {base: "."})
    .pipe(sourcemaps.init())
    .pipe(sassTask)
    .pipe(postcss(plugins))
    .pipe(sourcemaps.write())
    .pipe(rename(path => {
      //E.g. src\main\resources\sass\core -> src\main\resources\public\assets\static\css
      path.dirname = path.dirname.replace(/([\/\\])scss[\/\\]?/, '$1public$1assets$1static$1css');
    }))
    .pipe(gulp.dest('./'));
}

const babelOptions =
  {
    "exclude": "node_modules/**",
    "presets": [
      ["@babel/preset-env", {
        "useBuiltIns": "usage",
        "corejs": "3",
        "debug": false,
        "targets": {
          "chrome": 65,
          "ie": 11,
          "firefox": 52,
          "safari": 11,
          "ios": 12,
          "edge": 16
        }
      }]
    ],
    "plugins": ["@babel/plugin-transform-classes"],
  }

gulp.task('copyFdsResources', () => {
  return gulp.src(['fivium-design-system-core/fds/**/*'])
    .pipe(gulp.dest('src/main/resources/templates/fds'));
});

gulp.task('copyGovukResources', () => {
  return gulp.src(['fivium-design-system-core/node_modules/govuk-frontend/**/*'])
    .pipe(gulp.dest('src/main/resources/public/assets/govuk-frontend'));
});

gulp.task('copyFdsJs', () => {
  return gulp.src(['src/main/resources/templates/fds/static/js/**/*'])
    .pipe(gulp.dest('src/main/resources/public/assets/static/fds/js'));
});

gulp.task('copyFdsVendorJs', () => {
  return gulp.src(['src/main/resources/templates/fds/vendor/**/*'])
    .pipe(gulp.dest('src/main/resources/public/assets/static/js/vendor'))
});

gulp.task('copyHtml5Shiv', () => {
  return gulp.src(['fivium-design-system-core/node_modules/html5shiv/dist/html5shiv.min.js'])
    .pipe(gulp.dest('src/main/resources/public/assets/html5shiv'))
});

gulp.task('initFds', gulp.series(['copyFdsResources', 'copyGovukResources', 'copyHtml5Shiv', 'copyFdsJs', 'copyFdsVendorJs']));

gulp.task('sassCi', gulp.series(['initFds'], () => {
  return compileSass(true);
}));

gulp.task('buildAll', gulp.series(['sassCi']));


/*
john green
cse 3320
assignment 3 c
*/

#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <pthread.h>

struct entry  {
	char *string;
	float mag;
};
/* Globals for the threads to use */
struct entry **array;
int arraysize, tcount;

void *sort(void *num) {
	int i=0, j=0, id, start, end;
	struct entry *swap;

	/* find what portion or the array to sort */
	id = *((int*) num);
	start = id*(arraysize/tcount);
	if(id < tcount-1){
		end = id*(arraysize/tcount)+(arraysize/tcount);
	}
	else{
		end = arraysize;
	}
	/* bubble sort portion of array */
	for (i=start; i<end; i++){
		for (j=0; j<end-i-1; j++){
			if(array[j+start]->mag < array[j+1+start]->mag){
				swap = array[j+start];
				array[j+start] = array[j+1+start];
				array[j+1+start] = swap;
			}
		}
	}
	return 0;
}

void merge(){
	int i, j, tnum;
	int *ticker, *linecount;
	float max;
	FILE *fp;

	/* initialize index trackers */
	ticker = malloc(tcount*sizeof(int));
	linecount = malloc(tcount*sizeof(int));
	for(i=0; i<tcount; i++){
		ticker[i] = i*(arraysize/tcount);
		if(i < tcount-1){
			linecount[i] = i*(arraysize/tcount)+(arraysize/tcount);
		}
		else{
			linecount[i] = arraysize;
		}
	}
	/* merge the sorted portions of the array */
	fp = fopen("output.csv", "w+");
	for(i=0; i<arraysize; i++){
		max = -99;
		tnum = 0;
		for(j=0; j<tcount; j++){
			if((linecount[j] > ticker[j]) && (array[ticker[j]]->mag > max)){
				tnum = j;
				max = array[ticker[j]]->mag;
			}
		}
		fputs(array[ticker[tnum]]->string,fp);
		ticker[tnum] += 1;
	}
	fclose(fp);
}

int main(int argc, char *argv[]) {
	int count=0, numberOfChildren=0, ch=0, lines=0, i=0, *tid;
	float time=0;
	char buff[255], *tok;
	struct entry **shm;
	pthread_t *pth;
	clock_t start, diff;
	FILE *fp;

	/* get thread count from user */
	do{
		printf("how many children? ");
		fgets (buff, sizeof(buff), stdin);
		numberOfChildren = atoi(buff);
	} while ( numberOfChildren <= 0);

	/* get line count */
	fp = fopen("all_month.csv", "r");
	if (fp == NULL){
		printf("all_month.csv failed\n");
		return -1;
	}
	while(!feof(fp))
	{
		ch = fgetc(fp);
		if(ch == '\n')
		{
			lines++;
		}
	}
	fclose(fp);

	/* allocate array and id arrays */
	shm = malloc(lines*sizeof(shm));
	pth = malloc(numberOfChildren*sizeof(pthread_t));
	tid = malloc(numberOfChildren*sizeof(int));
	/* import data */
	fp = fopen("all_month.csv", "r");
	while ((fgets (buff, sizeof(buff), fp)) != NULL) {
		shm[count] = malloc(sizeof(shm));
		shm[count]->string = malloc(strlen(buff)+1);
		strcpy(shm[count]->string, buff);
		tok = strtok(buff, ",");
		tok = strtok(NULL, ",");
		tok = strtok(NULL, ",");
		tok = strtok(NULL, ",");
		tok = strtok(NULL, ",");
		if (buff[0] == 't'){
			shm[count]->mag = 100.0;
		}
		else {
			shm[count]->mag = atof(tok);
		}
		count++;
	}
	fclose(fp);

	/* assign global data */
	array = shm;
	arraysize = count;
	tcount = numberOfChildren;
	start = clock();

	/* start threads */
	for (i=0; i<numberOfChildren; i++){
		tid[i] = i;
		pthread_create(&pth[i],NULL,sort,&tid[i]);
	}

	/* wait for threads to end */
	for (i=0; i<numberOfChildren; i++){
		pthread_join(pth[i], NULL);
	}

	/* merge sorted array */
	merge();

	diff = clock()-start;
	time = ((float) diff)/ CLOCKS_PER_SEC;
	printf("time taken for %d processes = %.2f sec.\n", numberOfChildren, time);
	return 0;
}
